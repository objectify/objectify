package com.googlecode.objectify.cache.valkey;

import com.googlecode.objectify.cache.IdentifiableValue;
import com.googlecode.objectify.cache.MemcacheService;
import glide.api.GlideClient;
import glide.api.models.GlideString;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.SetOptions.ConditionalSet;
import lombok.RequiredArgsConstructor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static glide.api.models.GlideString.gs;

/**
 * A {@link MemcacheService} backed by Valkey 9.0+ using the valkey-glide client.
 *
 * <p>Compare-and-swap is implemented with the native {@code SET key value IFEQ comparison-value}
 * command introduced in Valkey 8.1 (and rounded out in 9.0 with companion commands like
 * {@code DELIFEQ}). This is one round trip per key, versus the multi-step
 * {@code WATCH/GET/MULTI/SET/EXEC} dance required on older Redis-compatible servers.</p>
 *
 * <p>Cold-cache handling: {@code IFEQ} requires the key to exist with the comparison value, so on
 * a miss {@link #getIdentifiables} bootstraps a single-byte null sentinel via {@code SET NX} and
 * then re-reads the key. The bootstrap value is what subsequent {@link #putIfUntouched} calls
 * compare against, mirroring the {@code add}-then-{@code gets} pattern used for memcached.</p>
 *
 * <p>Null values are stored as a single {@code 0x00} byte. Java-serialized objects always begin
 * with the magic bytes {@code 0xAC 0xED}, so the sentinel can never collide with a real value.</p>
 */
@RequiredArgsConstructor
public class ValkeyCacheService implements MemcacheService {

	/** Single-byte sentinel for null. Cannot collide with Java-serialized data (which starts 0xAC 0xED). */
	private static final byte[] NULL_VALUE = new byte[]{0};

	/** "OK" reply from Valkey when a write succeeds. */
	private static final String OK = "OK";

	private final GlideClient client;

	private static byte[] toCacheBytes(final Object thing) {
		if (thing == null) {
			return NULL_VALUE;
		}
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(thing);
			oos.flush();
			return baos.toByteArray();
		} catch (final IOException e) {
			throw new RuntimeException("Failed to serialize cache value", e);
		}
	}

	private static Object fromCacheBytes(final byte[] bytes) {
		if (bytes == null || Arrays.equals(bytes, NULL_VALUE)) {
			return null;
		}
		try (final ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
			 final ObjectInputStream ois = new ObjectInputStream(bais)) {
			return ois.readObject();
		} catch (final IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize cache value", e);
		}
	}

	private static GlideString gskey(final String key) {
		return gs(key.getBytes(StandardCharsets.UTF_8));
	}

	private static <T> T await(final java.util.concurrent.CompletableFuture<T> future) {
		try {
			return future.get();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (final ExecutionException e) {
			throw new RuntimeException(e.getCause());
		}
	}

	private byte[] rawGet(final String key) {
		final GlideString result = await(client.get(gskey(key)));
		return result == null ? null : result.getBytes();
	}

	@Override
	public Object get(final String key) {
		return fromCacheBytes(rawGet(key));
	}

	@Override
	public Map<String, IdentifiableValue> getIdentifiables(final Collection<String> keys) {
		final Map<String, IdentifiableValue> result = new LinkedHashMap<>();
		for (final String key : keys) {
			result.put(key, getIdentifiable(key));
		}
		return result;
	}

	private IdentifiableValue getIdentifiable(final String key) {
		final byte[] bytes = rawGet(key);
		if (bytes != null) {
			return new ValkeyIdentifiableValue(fromCacheBytes(bytes), bytes);
		}

		// Cold cache: bootstrap a sentinel under NX so we can later CAS against it. NX prevents
		// us from clobbering a value another caller has just set in between our GET and our SET.
		final SetOptions nx = SetOptions.builder()
				.conditionalSet(ConditionalSet.ONLY_IF_DOES_NOT_EXIST)
				.build();
		await(client.set(gskey(key), gs(NULL_VALUE), nx));

		final byte[] bootstrapped = rawGet(key);
		return bootstrapped == null ? null : new ValkeyIdentifiableValue(fromCacheBytes(bootstrapped), bootstrapped);
	}

	@Override
	public Map<String, Object> getAll(final Collection<String> keys) {
		final Map<String, Object> result = new LinkedHashMap<>();
		if (keys.isEmpty()) {
			return result;
		}

		final GlideString[] keyArray = keys.stream()
				.map(ValkeyCacheService::gskey)
				.toArray(GlideString[]::new);

		final Object[] values = await(client.mget(keyArray));

		int i = 0;
		for (final String key : keys) {
			final Object raw = values[i++];
			if (raw != null) {
				result.put(key, fromCacheBytes(((GlideString) raw).getBytes()));
			}
		}
		return result;
	}

	@Override
	public void put(final String key, final Object thing) {
		await(client.set(gskey(key), gs(toCacheBytes(thing))));
	}

	@Override
	public void putAll(final Map<String, Object> values) {
		if (values.isEmpty()) {
			return;
		}
		final Map<GlideString, GlideString> mapping = new LinkedHashMap<>();
		values.forEach((key, value) -> mapping.put(gskey(key), gs(toCacheBytes(value))));
		await(client.msetBinary(mapping));
	}

	/**
	 * Atomically writes new values only when the existing bytes still match the snapshot taken
	 * at {@link #getIdentifiables} time. Each key issues a single
	 * {@code SET key value IFEQ comparison-value [EX seconds]} command; Valkey performs the
	 * comparison server-side and either commits the write (returning {@code "OK"}) or aborts
	 * (returning {@code nil}).
	 */
	@Override
	public Set<String> putIfUntouched(final Map<String, CasPut> values) {
		final Set<String> successes = new HashSet<>();

		for (final Map.Entry<String, CasPut> entry : values.entrySet()) {
			final String key = entry.getKey();
			final CasPut casPut = entry.getValue();
			final ValkeyIdentifiableValue viv = (ValkeyIdentifiableValue) casPut.getIv();

			final byte[] expected = viv.getRawBytes();
			final byte[] next = toCacheBytes(casPut.getNextToStore());
			final int ttl = casPut.getExpirationSeconds();

			final GlideString[] args = ttl > 0
					? new GlideString[]{
							gs("SET"), gskey(key), gs(next),
							gs("IFEQ"), gs(expected),
							gs("EX"), gs(Integer.toString(ttl))}
					: new GlideString[]{
							gs("SET"), gskey(key), gs(next),
							gs("IFEQ"), gs(expected)};

			final Object response = await(client.customCommand(args));
			if (isOk(response)) {
				successes.add(key);
			}
		}

		return successes;
	}

	private static boolean isOk(final Object response) {
		if (response == null) {
			return false;
		}
		if (response instanceof String) {
			return OK.equals(response);
		}
		if (response instanceof GlideString) {
			return OK.equals(((GlideString) response).getString());
		}
		if (response instanceof byte[]) {
			return Arrays.equals(OK.getBytes(StandardCharsets.UTF_8), (byte[]) response);
		}
		return false;
	}

	@Override
	public void deleteAll(final Collection<String> keys) {
		if (keys.isEmpty()) {
			return;
		}
		final GlideString[] keyArray = keys.stream()
				.map(ValkeyCacheService::gskey)
				.toArray(GlideString[]::new);
		await(client.del(keyArray));
	}
}
