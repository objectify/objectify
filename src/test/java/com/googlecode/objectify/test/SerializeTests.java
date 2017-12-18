package com.googlecode.objectify.test;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.impl.ObjectifyImpl;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of the {@code @Serialize} annotation
 */
class SerializeTests extends TestBase {
	@Entity
	@Cache
	@Data
	private static class HasSerialize {
		@Id Long id;
		@Serialize Map<Long, Long> numbers = new HashMap<>();
	}

	@Test
	void simpleSerialize() throws Exception {
		factory().register(HasSerialize.class);

		final HasSerialize hs = new HasSerialize();
		hs.numbers.put(1L, 2L);
		hs.numbers.put(3L, 4L);

		final HasSerialize fetched = saveClearLoad(hs);
		assertThat(fetched).isEqualTo(hs);
	}

	/** */
	@Data
	private static class HasLongs {
		@Serialize long[] longs;
	}

	@Entity
	@Cache
	@Data
	private static class EmbedSerialize {
		@Id Long id;
		HasLongs simple;
	}

	@Test
	void testEmbedSerialize() throws Exception {
		factory().register(EmbedSerialize.class);

		final EmbedSerialize es = new EmbedSerialize();
		es.simple = new HasLongs();
		es.simple.longs = new long[] { 1L, 2L, 3L };

		final EmbedSerialize fetched = saveClearLoad(es);
		assertThat(fetched.simple.longs).isEqualTo(es.simple.longs);
	}

	@Entity(name="HasSerialize")
	@Cache
	@Data
	private static class HasSerializeZip {
		@Id Long id;
		@Serialize(zip=true) Map<Long, Long> numbers = new HashMap<>();
	}

	@Test
	void testSerializeZip() throws Exception {
		factory().register(HasSerializeZip.class);

		final HasSerializeZip hs = new HasSerializeZip();
		hs.numbers.put(1L, 2L);
		hs.numbers.put(3L, 4L);

		final HasSerializeZip fetched = saveClearLoad(hs);
		assertThat(fetched).isEqualTo(hs);
	}

	@Test
	void testSerializeZipButReadUnzip() throws Exception {
		factory().register(HasSerializeZip.class);

		final HasSerializeZip hs = new HasSerializeZip();
		hs.numbers.put(1L, 2L);
		hs.numbers.put(3L, 4L);

		ofy().save().entity(hs).now();

		// Now we need to read it using the non-zip annotation
		final ObjectifyFactory fact2 = new ObjectifyFactory(datastore(), memcache());
		fact2.register(HasSerialize.class);

		final ObjectifyImpl ofy2 = fact2.open();

		final HasSerialize fetched = ofy2.load().type(HasSerialize.class).id(hs.id).now();
		assertThat(fetched.numbers).isEqualTo(hs.numbers);

		ofy2.close();
	}

	@Test
	void testSerializeUnzipButReadZip() throws Exception {
		factory().register(HasSerialize.class);

		final HasSerialize hs = new HasSerialize();
		hs.numbers.put(1L, 2L);
		hs.numbers.put(3L, 4L);

		ofy().save().entity(hs).now();

		// Now we need to read it using the zip annotation
		final ObjectifyFactory fact2 = new ObjectifyFactory(datastore(), memcache());
		fact2.register(HasSerializeZip.class);

		final ObjectifyImpl ofy2 = fact2.open();
		final HasSerializeZip fetched = ofy2.load().type(HasSerializeZip.class).id(hs.id).now();
		assertThat(fetched.numbers).isEqualTo(hs.numbers);

		ofy2.close();
	}
}
