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
import java.io.InputStream;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

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
 * <p><b>Wire format.</b> The first byte of every stored value identifies its encoding, and the three
 * possibilities can never collide:
 * <ul>
 *   <li>{@code 0x00} — the null sentinel (a single byte; memcache couldn't store nulls either).</li>
 *   <li>{@code 0xAC} — an uncompressed Java-serialized object (the serialization stream magic is
 *       {@code 0xAC 0xED}).</li>
 *   <li>{@code 0x01} — a Deflate-compressed Java-serialized object; the compressed stream follows
 *       the marker byte.</li>
 * </ul>
 * Values are compressed only when their serialized form reaches {@link #COMPRESSION_THRESHOLD_BYTES}
 * (tiny values don't benefit and would just burn CPU on the write path). The memcache path
 * compressed only above ~16&nbsp;KB (spymemcached's default); this threshold is deliberately lower so
 * more of the keyspace is compressed, trading a little CPU on mid-size values for a materially
 * smaller keyspace — which matters more here, since a Valkey instance is bounded by {@code maxmemory}
 * rather than transparently evicting like memcache. Reads accept either form, so the cache stays
 * readable across a rollout and after a rollback — entries written before this change, and any
 * below the threshold, are plain uncompressed serializations.</p>
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

	/**
	 * Wraps the raw bytes of a stored value in the {@link ObjectInputStream} used to deserialize it.
	 * The default is a plain {@link ObjectInputStream}, which resolves classes by their exact stored
	 * name. Callers that need something else — most commonly a subclass overriding
	 * {@link ObjectInputStream#resolveClass} to map class names written by a differently-built process,
	 * such as one whose dependencies were relocated by a shading step — can supply their own. That lets
	 * processes with different relocation schemes share a single cache instead of each needing its own.
	 *
	 * <p>Kept as an explicit interface rather than a {@code Function<InputStream, ObjectInputStream>}
	 * because the {@link ObjectInputStream} constructor throws {@link IOException}.</p>
	 */
	@FunctionalInterface
	public interface ObjectInputStreamFactory {
		ObjectInputStream create(InputStream in) throws IOException;
	}

	/** Single-byte sentinel for null. Cannot collide with Java-serialized data (which starts 0xAC 0xED). */
	private static final byte[] NULL_VALUE = new byte[]{0};

	/**
	 * Marker byte prefixing a Deflate-compressed payload. Distinct from the null sentinel ({@code 0x00})
	 * and from the {@code 0xAC} that opens every Java serialization stream, so {@link #fromCacheBytes}
	 * can tell the three encodings apart from the first byte alone.
	 */
	private static final byte FORMAT_DEFLATE = 0x01;

	/**
	 * Serialized values at least this large are Deflate-compressed before storage; smaller ones are
	 * stored as-is. Set to 2&nbsp;KB — well below the ~16&nbsp;KB spymemcached's default transcoder used —
	 * to compress a larger share of the keyspace and relieve Valkey memory pressure; values under a couple
	 * KB compress poorly and aren't worth the CPU on the request hot path.
	 */
	public static final int COMPRESSION_THRESHOLD_BYTES = 2_048;

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

	/** Builds the {@link ObjectInputStream} used on the read path; see {@link ObjectInputStreamFactory}. */
	private final ObjectInputStreamFactory objectInputStreamFactory;

	/** Uses {@link #DEFAULT_EXPIRATION_SECONDS} as the fallback TTL and a plain {@link ObjectInputStream}. */
	public ValkeyCacheService(final BaseClient client) {
		this(client, DEFAULT_EXPIRATION_SECONDS);
	}

	/** Uses {@link #DEFAULT_EXPIRATION_SECONDS} as the fallback TTL. */
	public ValkeyCacheService(final BaseClient client, final ObjectInputStreamFactory objectInputStreamFactory) {
		this(client, DEFAULT_EXPIRATION_SECONDS, objectInputStreamFactory);
	}

	/** Uses a plain {@link ObjectInputStream} on the read path. */
	public ValkeyCacheService(final BaseClient client, final int defaultExpirationSeconds) {
		this(client, defaultExpirationSeconds, ObjectInputStream::new);
	}

	public ValkeyCacheService(
			final BaseClient client,
			final int defaultExpirationSeconds,
			final ObjectInputStreamFactory objectInputStreamFactory) {
		if (defaultExpirationSeconds <= 0) {
			throw new IllegalArgumentException("defaultExpirationSeconds must be positive, got " + defaultExpirationSeconds);
		}
		this.client = client;
		this.defaultExpirationSeconds = defaultExpirationSeconds;
		this.objectInputStreamFactory = Objects.requireNonNull(objectInputStreamFactory, "objectInputStreamFactory");
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
		final byte[] serialized = serialize(thing);
		// Compress only above the threshold: below it Deflate rarely pays for itself (and can even grow
		// tiny payloads) while still costing CPU on the write hot path. Above it, compression keeps large
		// entities from taking a disproportionate share of the server's memory.
		if (serialized.length < COMPRESSION_THRESHOLD_BYTES) {
			return serialized;
		}
		return deflate(serialized);
	}

	private Object fromCacheBytes(final byte[] bytes) {
		if (bytes == null || Arrays.equals(bytes, NULL_VALUE)) {
			return null;
		}
		// The first byte identifies the encoding (see the class javadoc). A FORMAT_DEFLATE marker means the
		// remainder is a compressed stream; anything else is a bare Java serialization — including every
		// entry written before compression existed — so old and new values both read back correctly.
		final byte[] serialized = (bytes.length > 0 && bytes[0] == FORMAT_DEFLATE)
				? inflate(bytes, 1, bytes.length - 1)
				: bytes;
		return deserialize(serialized);
	}

	private static byte[] serialize(final Object thing) {
		try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
			 final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(thing);
			oos.flush();
			return baos.toByteArray();
		} catch (final IOException e) {
			throw new RuntimeException("Failed to serialize cache value", e);
		}
	}

	private Object deserialize(final byte[] serialized) {
		try (final ByteArrayInputStream bais = new ByteArrayInputStream(serialized);
			 final ObjectInputStream ois = objectInputStreamFactory.create(bais)) {
			return ois.readObject();
		} catch (final IOException | ClassNotFoundException e) {
			throw new RuntimeException("Failed to deserialize cache value", e);
		}
	}

	/** Deflates {@code data}, prefixing the {@link #FORMAT_DEFLATE} marker so reads can spot the encoding. */
	private static byte[] deflate(final byte[] data) {
		final Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION);
		try {
			deflater.setInput(data);
			deflater.finish();
			// Compressed output is normally well under the input size; the marker byte rides along in front.
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, data.length / 3));
			baos.write(FORMAT_DEFLATE);
			final byte[] buffer = new byte[8192];
			while (!deflater.finished()) {
				final int n = deflater.deflate(buffer);
				baos.write(buffer, 0, n);
			}
			return baos.toByteArray();
		} finally {
			deflater.end();
		}
	}

	/** Inflates the {@code length} bytes at {@code offset} (the payload after the {@link #FORMAT_DEFLATE} marker). */
	private static byte[] inflate(final byte[] data, final int offset, final int length) {
		final Inflater inflater = new Inflater();
		try {
			inflater.setInput(data, offset, length);
			final ByteArrayOutputStream baos = new ByteArrayOutputStream(Math.max(64, length * 3));
			final byte[] buffer = new byte[8192];
			while (!inflater.finished()) {
				final int n = inflater.inflate(buffer);
				if (n == 0 && (inflater.needsInput() || inflater.needsDictionary())) {
					throw new IllegalStateException("Corrupt or truncated compressed cache value");
				}
				baos.write(buffer, 0, n);
			}
			return baos.toByteArray();
		} catch (final DataFormatException e) {
			throw new RuntimeException("Failed to decompress cache value", e);
		} finally {
			inflater.end();
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
