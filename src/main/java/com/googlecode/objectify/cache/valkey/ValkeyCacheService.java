package com.googlecode.objectify.cache.valkey;

import com.googlecode.objectify.cache.IdentifiableValue;
import com.googlecode.objectify.cache.MemcacheService;
import glide.api.BaseClient;
import glide.api.GlideClient;
import glide.api.GlideClusterClient;
import glide.api.models.GlideString;
import glide.api.models.commands.SetOptions;
import glide.api.models.commands.SetOptions.ConditionalSet;
import glide.api.models.configuration.RequestRoutingConfiguration.Route;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotKeyRoute;
import glide.api.models.configuration.RequestRoutingConfiguration.SlotType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static glide.api.models.GlideString.gs;

/**
 * A {@link MemcacheService} backed by Valkey 8.1+ using the valkey-glide client.
 *
 * <p>Works against both a standalone server and a cluster: the client is held as a
 * {@link BaseClient} (either a {@code GlideClient} or a {@code GlideClusterClient}), and every
 * operation is single-key or routed by key. Multi-key reads/writes/deletes are issued as
 * independent per-key commands fired concurrently rather than {@code MGET}/{@code MSET}/{@code DEL
 * key...}, which would fail with {@code CROSSSLOT} on a cluster.</p>
 *
 * <p>Compare-and-swap uses the native {@code SET key value IFEQ comparison-value} command
 * introduced in Valkey 8.1 — one round trip per key, with the comparison performed server-side. On
 * a cluster the command is routed explicitly to the primary owning the key's slot
 * ({@link SlotKeyRoute}); on standalone it is issued directly.</p>
 *
 * <p>Cold-cache handling: {@code IFEQ} requires the key to exist with the comparison value, so on
 * a miss {@link #getIdentifiables} bootstraps a single-byte null sentinel via {@code SET NX} and
 * then re-reads the key. The bootstrap value is what subsequent {@link #putIfUntouched} calls
 * compare against, mirroring the {@code add}-then-{@code gets} pattern used for memcached.</p>
 *
 * <p>Null values are stored as a single {@code 0x00} byte. Java-serialized objects always begin
 * with the magic bytes {@code 0xAC 0xED}, so the sentinel can never collide with a real value.</p>
 *
 * <p><b>Expiration.</b> Every write carries a TTL so the keyspace stays bounded. Memcache-backed
 * caches shed cold entries via LRU eviction; a Valkey cluster configured with {@code noeviction}
 * cannot, so a persistent write pattern grows until {@code maxmemory} is hit and every subsequent
 * write (including unrelated callers sharing the cluster) fails with {@code OOM}. Entities that
 * declare their own expiration keep it; everything else — plain {@link #put}/{@link #putAll}
 * writes, the cold-cache null sentinel, and CAS writes with no explicit TTL — falls back to
 * {@code defaultExpirationSeconds}.</p>
 */
public class ValkeyCacheService implements MemcacheService {

	/** Single-byte sentinel for null. Cannot collide with Java-serialized data (which starts 0xAC 0xED). */
	private static final byte[] NULL_VALUE = new byte[]{0};

	/** "OK" reply from Valkey when a write succeeds. */
	private static final String OK = "OK";

	/** Default TTL applied to writes without an explicit expiration: 24 hours. */
	public static final int DEFAULT_EXPIRATION_SECONDS = 86_400;

	/**
	 * BaseClient so this works with either the standalone (GlideClient) or cluster
	 * (GlideClusterClient) valkey client; every operation is single-key or key-routed.
	 */
	private final BaseClient client;

	/** Fallback TTL (seconds) for writes that don't specify their own; keeps the keyspace bounded. */
	private final int defaultExpirationSeconds;

	/**
	 * SET options that expire the key after {@link #defaultExpirationSeconds}. {@link SetOptions} is immutable and
	 * thread-safe once built, so we pre-build one instance instead of allocating a new builder on every
	 * {@link #put}/{@link #putAll} — both hot paths for a cache service.
	 */
	private final SetOptions defaultSetOptions;

	/** Cold-cache sentinel options: {@code NX} plus the default TTL. Pre-built for the same reason as {@link #defaultSetOptions}. */
	private final SetOptions defaultNxSetOptions;

	/** Uses {@link #DEFAULT_EXPIRATION_SECONDS} as the fallback TTL. */
	public ValkeyCacheService(final BaseClient client) {
		this(client, DEFAULT_EXPIRATION_SECONDS);
	}

	public ValkeyCacheService(final BaseClient client, final int defaultExpirationSeconds) {
		if (defaultExpirationSeconds <= 0) {
			throw new IllegalArgumentException("defaultExpirationSeconds must be positive, got " + defaultExpirationSeconds);
		}
		this.client = client;
		this.defaultExpirationSeconds = defaultExpirationSeconds;
		this.defaultSetOptions = SetOptions.builder()
				.expiry(SetOptions.Expiry.Seconds((long) defaultExpirationSeconds))
				.build();
		this.defaultNxSetOptions = SetOptions.builder()
				.conditionalSet(ConditionalSet.ONLY_IF_DOES_NOT_EXIST)
				.expiry(SetOptions.Expiry.Seconds((long) defaultExpirationSeconds))
				.build();
	}

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

	private static <T> T await(final CompletableFuture<T> future) {
		try {
			return future.get();
		} catch (final InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new RuntimeException(e);
		} catch (final ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	private byte[] rawGet(final String key) {
		final GlideString value = await(client.get(gskey(key)));
		return value == null ? null : value.getBytes();
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
		// TTL-bounded like every other write (defaultNxSetOptions): a read-heavy workload bootstraps
		// a sentinel per cold key, and without expiry those persist forever on a noeviction cluster.
		await(client.set(gskey(key), gs(NULL_VALUE), defaultNxSetOptions));

		final byte[] bootstrapped = rawGet(key);
		return bootstrapped == null ? null : new ValkeyIdentifiableValue(fromCacheBytes(bootstrapped), bootstrapped);
	}

	@Override
	public Map<String, Object> getAll(final Collection<String> keys) {
		final Map<String, Object> result = new LinkedHashMap<>();
		if (keys.isEmpty()) {
			return result;
		}

		// Per-key GETs fired concurrently (no MGET, which would CROSSSLOT on a cluster).
		final Map<String, CompletableFuture<GlideString>> futures = new LinkedHashMap<>();
		for (final String key : keys) {
			futures.put(key, client.get(gskey(key)));
		}
		futures.forEach((key, future) -> {
			final GlideString value = await(future);
			if (value != null) {
				result.put(key, fromCacheBytes(value.getBytes()));
			}
		});
		return result;
	}

	@Override
	public void put(final String key, final Object thing) {
		await(client.set(gskey(key), gs(toCacheBytes(thing)), defaultSetOptions));
	}

	@Override
	public void putAll(final Map<String, Object> values) {
		if (values.isEmpty()) {
			return;
		}
		// Per-key SETs fired concurrently (no MSET, which would CROSSSLOT on a cluster).
		final List<CompletableFuture<String>> futures = new ArrayList<>();
		values.forEach((key, value) -> futures.add(client.set(gskey(key), gs(toCacheBytes(value)), defaultSetOptions)));
		futures.forEach(ValkeyCacheService::await);
	}

	/**
	 * Atomically writes new values only when the existing bytes still match the snapshot taken at
	 * {@link #getIdentifiables} time. Each key issues a single
	 * {@code SET key value IFEQ comparison-value [EX seconds]} command; Valkey performs the
	 * comparison server-side and either commits the write (returning {@code "OK"}) or aborts
	 * (returning {@code nil}). On a cluster each command is routed to the primary owning the key.
	 */
	@Override
	public Set<String> putIfUntouched(final Map<String, CasPut> values) {
		final Map<String, CompletableFuture<Boolean>> futures = new LinkedHashMap<>();
		for (final Map.Entry<String, CasPut> entry : values.entrySet()) {
			final String key = entry.getKey();
			final CasPut casPut = entry.getValue();
			final ValkeyIdentifiableValue viv = (ValkeyIdentifiableValue) casPut.getIv();

			final GlideString expected = gs(viv.getRawBytes());
			final GlideString next = gs(toCacheBytes(casPut.getNextToStore()));
			// Honor an entity's own expiration; otherwise fall back to the default TTL so CAS writes
			// stay bounded like plain puts (0 previously meant "never expire").
			final int explicitTtl = casPut.getExpirationSeconds();
			final int ttl = explicitTtl > 0 ? explicitTtl : defaultExpirationSeconds;

			final GlideString[] args = new GlideString[]{
					gs("SET"), gskey(key), next,
					gs("IFEQ"), expected,
					gs("EX"), gs(Integer.toString(ttl))};

			futures.put(key, casAsync(key, args));
		}

		final Set<String> successes = new HashSet<>();
		futures.forEach((key, future) -> {
			if (Boolean.TRUE.equals(await(future))) {
				successes.add(key);
			}
		});
		return successes;
	}

	/**
	 * Issues a custom {@code SET ... IFEQ} command, routing it to the key's slot on a cluster.
	 * {@code customCommand} is not part of {@link BaseClient} and returns a {@code ClusterValue} on
	 * the cluster client, so the two client types are handled explicitly here.
	 */
	private CompletableFuture<Boolean> casAsync(final String key, final GlideString[] args) {
		if (client instanceof GlideClusterClient) {
			final Route route = new SlotKeyRoute(key, SlotType.PRIMARY);
			return ((GlideClusterClient) client).customCommand(args, route)
					.thenApply(clusterValue -> isOk(clusterValue.getSingleValue()));
		}
		return ((GlideClient) client).customCommand(args).thenApply(ValkeyCacheService::isOk);
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
		// Per-key DELs fired concurrently (no multi-key DEL, which would CROSSSLOT on a cluster).
		final List<CompletableFuture<Long>> futures = new ArrayList<>();
		for (final String key : keys) {
			futures.add(client.del(new GlideString[]{gskey(key)}));
		}
		futures.forEach(ValkeyCacheService::await);
	}
}
