package com.googlecode.objectify.test.valkey;

import com.googlecode.objectify.cache.IdentifiableValue;
import com.googlecode.objectify.cache.MemcacheService;
import com.googlecode.objectify.cache.MemcacheService.CasPut;
import com.googlecode.objectify.cache.valkey.ValkeyCacheService;
import com.googlecode.objectify.cache.valkey.ValkeyIdentifiableValue;
import glide.api.GlideClient;
import glide.api.models.GlideString;
import glide.api.models.configuration.GlideClientConfiguration;
import glide.api.models.configuration.NodeAddress;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.google.common.truth.Truth.assertThat;

/**
 * Direct unit tests for {@link ValkeyCacheService}, covering the behavior the higher-level
 * {@code EntityMemcache}-driven tests don't exercise: the null sentinel on cold-cache reads,
 * the {@code SET ... IFEQ} CAS contract for both winners and losers, native {@code EX seconds}
 * expiration, and a multi-threaded race where only one CAS may win.
 */
class ValkeyCacheServiceTests {

	@SuppressWarnings("resource")
	private static final GenericContainer<?> VALKEY = new GenericContainer<>(DockerImageName.parse("valkey/valkey:9.0"))
			.withExposedPorts(6379);

	private static GlideClient client;
	private MemcacheService cache;

	@BeforeAll
	static void startValkey() throws Exception {
		VALKEY.start();
		client = GlideClient.createClient(
				GlideClientConfiguration.builder()
						.address(NodeAddress.builder().host(VALKEY.getHost()).port(VALKEY.getMappedPort(6379)).build())
						.requestTimeout(5000)
						.build()
		).get();
	}

	@AfterAll
	static void stopValkey() throws Exception {
		if (client != null) {
			client.close();
		}
		VALKEY.stop();
	}

	@BeforeEach
	void flush() throws Exception {
		client.customCommand(new String[]{"FLUSHALL"}).get();
		cache = new ValkeyCacheService(client);
	}

	@Test
	void putAndGetRoundTripsArbitrarySerializable() {
		final Sample value = new Sample("hello", 42);
		cache.put("k", value);
		assertThat(cache.get("k")).isEqualTo(value);
	}

	@Test
	void getAbsentKeyReturnsNull() {
		assertThat(cache.get("nope")).isNull();
	}

	@Test
	void putNullStoresAndReadsBackAsNull() {
		cache.put("k", null);
		assertThat(cache.get("k")).isNull();
	}

	@Test
	void getAllSkipsAbsentKeys() {
		cache.put("a", "alpha");
		cache.put("c", "gamma");

		final Map<String, Object> result = cache.getAll(Arrays.asList("a", "b", "c"));
		assertThat(result).containsExactly("a", "alpha", "c", "gamma");
	}

	@Test
	void putAllWritesEverything() {
		final Map<String, Object> in = new LinkedHashMap<>();
		in.put("a", "alpha");
		in.put("b", null);
		cache.putAll(in);

		assertThat(cache.get("a")).isEqualTo("alpha");
		assertThat(cache.get("b")).isNull();
	}

	@Test
	void deleteAllRemovesKeys() {
		cache.put("a", "alpha");
		cache.put("b", "beta");
		cache.deleteAll(Arrays.asList("a", "b"));
		assertThat(cache.get("a")).isNull();
		assertThat(cache.get("b")).isNull();
	}

	@Test
	void getIdentifiablesBootstrapsColdCacheWithNullSentinel() {
		final Map<String, IdentifiableValue> ivs = cache.getIdentifiables(Arrays.asList("cold"));
		final IdentifiableValue iv = ivs.get("cold");
		assertThat(iv).isNotNull();
		assertThat(iv.getValue()).isNull();   // sentinel decodes back to null
	}

	@Test
	void casSucceedsOnUntouchedSentinel() {
		final IdentifiableValue iv = cache.getIdentifiables(Arrays.asList("k")).get("k");

		final Set<String> winners = cache.putIfUntouched(
				Map.of("k", new CasPut(iv, "fresh", 0)));

		assertThat(winners).containsExactly("k");
		assertThat(cache.get("k")).isEqualTo("fresh");
	}

	@Test
	void casFailsAfterAnotherWriterChangedTheValue() {
		cache.put("k", "original");
		final IdentifiableValue iv = cache.getIdentifiables(Arrays.asList("k")).get("k");
		assertThat(iv.getValue()).isEqualTo("original");

		// Simulate a competing writer.
		cache.put("k", "stomped");

		final Set<String> winners = cache.putIfUntouched(
				Map.of("k", new CasPut(iv, "fresh", 0)));

		assertThat(winners).isEmpty();
		assertThat(cache.get("k")).isEqualTo("stomped");
	}

	@Test
	void casReplacesExistingValueWhenUntouched() {
		cache.put("k", "first");
		final IdentifiableValue iv = cache.getIdentifiables(Arrays.asList("k")).get("k");

		final Set<String> winners = cache.putIfUntouched(
				Map.of("k", new CasPut(iv, "second", 0)));

		assertThat(winners).containsExactly("k");
		assertThat(cache.get("k")).isEqualTo("second");
	}

	@Test
	void putAppliesDefaultTtl() throws Exception {
		final MemcacheService ttlCache = new ValkeyCacheService(client, 100);
		ttlCache.put("k", "v");
		assertThat(ttlOf("k")).isGreaterThan(0L);
	}

	@Test
	void putAllAppliesDefaultTtl() throws Exception {
		final MemcacheService ttlCache = new ValkeyCacheService(client, 100);
		final Map<String, Object> in = new LinkedHashMap<>();
		in.put("a", "alpha");
		ttlCache.putAll(in);
		assertThat(ttlOf("a")).isGreaterThan(0L);
	}

	@Test
	void coldCacheSentinelAppliesDefaultTtl() throws Exception {
		final MemcacheService ttlCache = new ValkeyCacheService(client, 100);
		ttlCache.getIdentifiables(Arrays.asList("cold"));
		assertThat(ttlOf("cold")).isGreaterThan(0L);
	}

	@Test
	void casFallsBackToDefaultTtlWhenUnset() throws Exception {
		final MemcacheService ttlCache = new ValkeyCacheService(client, 100);
		final IdentifiableValue iv = ttlCache.getIdentifiables(Arrays.asList("k")).get("k");
		assertThat(ttlCache.putIfUntouched(Map.of("k", new CasPut(iv, "v", 0)))).containsExactly("k");
		assertThat(ttlOf("k")).isGreaterThan(0L);
	}

	@Test
	void rejectsNonPositiveDefaultTtl() {
		try {
			new ValkeyCacheService(client, 0);
			throw new AssertionError("expected IllegalArgumentException");
		} catch (final IllegalArgumentException expected) {
			// expected
		}
	}

	/** Remaining TTL (seconds) on a key, or a negative sentinel (-1 no expiry, -2 absent) per the Valkey TTL contract. */
	private long ttlOf(final String key) throws Exception {
		return ((Number) client.customCommand(new String[]{"TTL", key}).get()).longValue();
	}

	@Test
	void casHonorsExpirationSeconds() throws Exception {
		final IdentifiableValue iv = cache.getIdentifiables(Arrays.asList("k")).get("k");

		assertThat(cache.putIfUntouched(Map.of("k", new CasPut(iv, "ttl-value", 1))))
				.containsExactly("k");
		assertThat(cache.get("k")).isEqualTo("ttl-value");

		Thread.sleep(2000);

		assertThat(cache.get("k")).isNull();
	}

	@Test
	void putIfUntouchedReportsPartialSuccess() {
		final Map<String, IdentifiableValue> ivs = cache.getIdentifiables(Arrays.asList("a", "b"));

		// Stomp only on "a"; "b" should still CAS successfully.
		cache.put("a", "stomped");

		final Map<String, CasPut> proposed = new LinkedHashMap<>();
		proposed.put("a", new CasPut(ivs.get("a"), "fresh-a", 0));
		proposed.put("b", new CasPut(ivs.get("b"), "fresh-b", 0));

		final Set<String> winners = cache.putIfUntouched(proposed);

		assertThat(winners).containsExactly("b");
		assertThat(cache.get("a")).isEqualTo("stomped");
		assertThat(cache.get("b")).isEqualTo("fresh-b");
	}

	/**
	 * Many threads racing on the same key — exactly one CAS must win, and the cache must end up
	 * holding the winner's value. Verifies that the CAS isn't subject to lost-update races even
	 * under contention, which is the property memcache CAS originally provided and that the
	 * Valkey {@code IFEQ} path needs to preserve.
	 */
	@Test
	void casUnderContentionProducesExactlyOneWinner() throws Exception {
		final String key = "contended-" + UUID.randomUUID();
		cache.put(key, "seed");

		final int writers = 10;
		final ExecutorService pool = Executors.newFixedThreadPool(writers);
		final CountDownLatch start = new CountDownLatch(1);
		final AtomicInteger wins = new AtomicInteger();

		try {
			for (int i = 0; i < writers; i++) {
				final String myValue = "writer-" + i;
				pool.submit(() -> {
					try {
						start.await();
						final IdentifiableValue iv = cache.getIdentifiables(Arrays.asList(key)).get(key);
						final Set<String> won = cache.putIfUntouched(
								Map.of(key, new CasPut(iv, myValue, 0)));
						if (won.contains(key)) {
							wins.incrementAndGet();
						}
					} catch (final InterruptedException ie) {
						Thread.currentThread().interrupt();
					}
					return null;
				});
			}
			start.countDown();
		} finally {
			pool.shutdown();
			assertThat(pool.awaitTermination(15, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(wins.get()).isEqualTo(1);
		assertThat(cache.get(key)).isInstanceOf(String.class);
		assertThat((String) cache.get(key)).startsWith("writer-");
	}

	@Test
	void largeValueRoundTripsThroughCompression() throws Exception {
		final String big = repeat('x', 200_000);
		cache.put("big", big);

		// Round-trips intact...
		assertThat(cache.get("big")).isEqualTo(big);

		// ...and was actually stored compressed: the deflate marker leads, and the stored form is far
		// smaller than the raw serialization.
		final byte[] stored = rawBytes("big");
		assertThat(stored[0] & 0xFF).isEqualTo(0x01);
		assertThat(stored.length).isLessThan(javaSerialize(big).length);
	}

	@Test
	void smallValueIsStoredUncompressed() throws Exception {
		cache.put("small", new Sample("hello", 42));

		// Below the threshold we skip compression, so the stored bytes are a bare Java serialization
		// (stream magic 0xAC), not a compressed payload.
		final byte[] stored = rawBytes("small");
		assertThat(stored[0] & 0xFF).isEqualTo(0xAC);
	}

	@Test
	void midSizeValueAboveThresholdIsCompressed() throws Exception {
		// ~4 KB serialized: below the old 16 KB memcache cutoff but above the current 2 KB threshold,
		// so it must now be compressed. Guards the lowered threshold against silently regressing to 16 KB.
		final String mid = repeat('z', 4_000);
		assertThat(javaSerialize(mid).length).isGreaterThan(ValkeyCacheService.COMPRESSION_THRESHOLD_BYTES);
		cache.put("mid", mid);

		assertThat(cache.get("mid")).isEqualTo(mid);
		final byte[] stored = rawBytes("mid");
		assertThat(stored[0] & 0xFF).isEqualTo(0x01);
	}

	@Test
	void readsLegacyUncompressedValueWrittenBeforeCompression() throws Exception {
		// An entry written by the pre-compression code is a raw Java serialization with no marker byte.
		final Sample value = new Sample("legacy", 7);
		writeRaw("legacy", javaSerialize(value));
		assertThat(cache.get("legacy")).isEqualTo(value);
	}

	@Test
	void readsLegacyLargeUncompressedValue() throws Exception {
		// A large entry written before compression existed is stored uncompressed; the read path must not
		// assume "large implies compressed", so this must deserialize directly rather than try to inflate.
		final String big = repeat('y', 200_000);
		writeRaw("legacy-big", javaSerialize(big));
		assertThat(cache.get("legacy-big")).isEqualTo(big);
	}

	@Test
	void readsEmptyStoredValueAsCorruptRatherThanIndexError() throws Exception {
		// A zero-length value can't come from our own writes, but an external writer could leave one.
		// It must not trip an ArrayIndexOutOfBoundsException on the format-byte check; it falls through
		// to deserialization, which wraps the failure descriptively.
		writeRaw("empty", new byte[0]);
		try {
			cache.get("empty");
			throw new AssertionError("expected a wrapped deserialization failure");
		} catch (final RuntimeException expected) {
			assertThat(expected).isNotInstanceOf(ArrayIndexOutOfBoundsException.class);
		}
	}

	@Test
	void injectedFactoryRemapsClassNamesOnRead() {
		// Simulates the split you get when one process writes with shaded (relocated) dependencies and
		// another reads with un-relocated ones: a value serialized under one class name is read back by a
		// process that only has the class under a different name, which otherwise surfaces as a
		// ClassNotFoundException. A ValkeyCacheService built with a custom ObjectInputStreamFactory that
		// swaps the class descriptor must decode it. The swap is done in readClassDescriptor(), not
		// resolveClass(): since JDK 17 the latter rejects a class whose name differs from the stream
		// descriptor with InvalidClassException.
		final String writtenName = Alpha.class.getName();
		final MemcacheService remapping = new ValkeyCacheService(client, in -> new ObjectInputStream(in) {
			@Override
			protected ObjectStreamClass readClassDescriptor() throws IOException, ClassNotFoundException {
				final ObjectStreamClass desc = super.readClassDescriptor();
				return desc.getName().equals(writtenName) ? ObjectStreamClass.lookup(Beta.class) : desc;
			}
		});

		remapping.put("k", new Alpha("payload"));
		final Object read = remapping.get("k");

		assertThat(read).isInstanceOf(Beta.class);
		assertThat(((Beta) read).s).isEqualTo("payload");
	}

	@Test
	void factoryConstructorRoundTripsAndAppliesDefaultTtl() throws Exception {
		// The (client, ttl, factory) constructor honors both the TTL and a plain-delegating factory.
		final MemcacheService withFactory =
				new ValkeyCacheService(client, 100, ObjectInputStream::new);
		withFactory.put("k", new Alpha("v"));
		assertThat(((Alpha) withFactory.get("k")).s).isEqualTo("v");
		assertThat(ttlOf("k")).isGreaterThan(0L);
	}

	private static byte[] rawBytes(final String key) throws Exception {
		final GlideString value = client.get(GlideString.gs(key.getBytes(StandardCharsets.UTF_8))).get();
		return value == null ? null : value.getBytes();
	}

	private static void writeRaw(final String key, final byte[] bytes) throws Exception {
		client.set(GlideString.gs(key.getBytes(StandardCharsets.UTF_8)), GlideString.gs(bytes)).get();
	}

	private static byte[] javaSerialize(final Object thing) throws Exception {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (final ObjectOutputStream oos = new ObjectOutputStream(baos)) {
			oos.writeObject(thing);
		}
		return baos.toByteArray();
	}

	private static String repeat(final char c, final int n) {
		final char[] chars = new char[n];
		Arrays.fill(chars, c);
		return new String(chars);
	}

	/**
	 * {@link Alpha} and {@link Beta} share an identical serial layout and {@code serialVersionUID}, so a
	 * stream written as {@code Alpha} deserializes into {@code Beta} once {@code resolveClass} maps the
	 * name — the same compatibility the shaded/un-relocated Guava class pairs rely on.
	 */
	private static class Alpha implements Serializable {
		private static final long serialVersionUID = 99L;
		private final String s;

		Alpha(final String s) {
			this.s = s;
		}
	}

	private static class Beta implements Serializable {
		private static final long serialVersionUID = 99L;
		private final String s;

		Beta(final String s) {
			this.s = s;
		}
	}

	private static class Sample implements Serializable {
		private static final long serialVersionUID = 1L;
		private final String s;
		private final int n;

		Sample(final String s, final int n) {
			this.s = s;
			this.n = n;
		}

		@Override
		public boolean equals(final Object o) {
			if (!(o instanceof Sample)) return false;
			final Sample other = (Sample) o;
			return n == other.n && s.equals(other.s);
		}

		@Override
		public int hashCode() {
			return s.hashCode() * 31 + n;
		}
	}
}
