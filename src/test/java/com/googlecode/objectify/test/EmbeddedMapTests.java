package com.googlecode.objectify.test;

import com.google.cloud.datastore.DatastoreException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.stringifier.KeyStringifier;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test of expando-type maps that hold primitve values
 */
class EmbeddedMapTests extends TestBase
{
	@Entity
	private static class HasMapLong {
		@Id
		Long id;
		Map<String, Long> primitives = new HashMap<>();
	}

	@Test
	void valuesCanBePrimitives() throws Exception {
		factory().register(HasMapLong.class);

		final HasMapLong hml = new HasMapLong();
		hml.primitives.put("one", 1L);
		hml.primitives.put("two", 2L);

		final HasMapLong fetched = saveClearLoad(hml);

		assertThat(fetched.primitives).isEqualTo(hml.primitives);
	}

	/**
	 * This changed somewhere between google cloud java version 1.16 and 1.74. Apparently you can now
	 * put dots in names.
	 */
//	@Test
	void dotsAreNotAllowed() {
		factory().register(HasMapLong.class);

		final HasMapLong hml = new HasMapLong();
		hml.primitives.put("legal.name", 123L);

		assertThrows(DatastoreException.class, () -> {
			final HasMapLong fetched = saveClearLoad(hml);
			//assertThat(fetched.primitives).isEqualTo(hml.primitives);	// this doesn't work anymore, we throw
		});
	}

	@Test
	void nullKeysAreForbidden() {
		factory().register(HasMapLong.class);

		final HasMapLong hml = new HasMapLong();
		hml.primitives.put(null, 123L);

		assertThrows(SaveException.class, () -> ofy().save().entity(hml).now());
	}

	@Test
	void nullValuesWork() throws Exception {
		factory().register(HasMapLong.class);

		final HasMapLong hml = new HasMapLong();
		hml.primitives.put("nullvalue", null);

		final HasMapLong fetched = saveClearLoad(hml);
		assertThat(fetched.primitives).isEqualTo(hml.primitives);;
	}

	/**
	 * We should be able to store a Key<?> as the EmbedMap key using a Stringifier
	 */
	@Entity
	private static class HasMapWithKeyKey {
		@Id
		private Long id;

		@Stringify(KeyStringifier.class)
		private Map<Key<Trivial>, Long> primitives = new HashMap<>();
	}

	@Test
	void keyStringifierWorks() throws Exception {
		factory().register(HasMapWithKeyKey.class);

		final HasMapWithKeyKey hml = new HasMapWithKeyKey();
		hml.primitives.put(Key.create(Trivial.class, 123L), 1L);
		hml.primitives.put(Key.create(Trivial.class, 456L), 2L);

		final HasMapWithKeyKey fetched = saveClearLoad(hml);

		assertThat(fetched.primitives).isEqualTo(hml.primitives);
	}

	@Entity
	private static class MapWithSetIssue {
		@Id
		private Long id;

		@Index
		private Map<String, Set<String>> mapWithSet = new HashMap<>();
	}

	@Test
	void testMapWithSet() throws Exception {
		factory().register(MapWithSetIssue.class);

		final MapWithSetIssue mws = new MapWithSetIssue();
		mws.mapWithSet.put("key", Collections.singleton("value"));

		final MapWithSetIssue fetched = saveClearLoad(mws);

		assertThat(fetched.mapWithSet).isEqualTo(mws.mapWithSet);
	}


	//
	//
	//

	private enum SomeEnum { ONE, TWO }

	@Entity
	private static class HasEnumKeyMap {
		@Id
		private Long id;
		private Map<SomeEnum, Long> primitives = new HashMap<>();
	}

	@Test
	void keyCanBeEnum() throws Exception {
		factory().register(HasEnumKeyMap.class);

		final HasEnumKeyMap he = new HasEnumKeyMap();
		he.primitives.put(SomeEnum.ONE, 1L);
		he.primitives.put(SomeEnum.TWO, 2L);

		final HasEnumKeyMap fetched = saveClearLoad(he);

		assertThat(fetched.primitives).isEqualTo(he.primitives);
	}

}
