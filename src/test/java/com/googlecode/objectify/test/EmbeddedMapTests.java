package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.stringifier.KeyStringifier;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Test of expando-type maps that hold primitve values
 */
public class EmbeddedMapTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapLong {
		@Id
		Long id;
		Map<String, Long> primitives = new HashMap<>();
	}

	@Test
	public void valuesCanBePrimitives() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("one", 1L);
		hml.primitives.put("two", 2L);

		HasMapLong fetched = ofy().saveClearLoad(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Test
	public void dotsAreAllowed() {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("legal.name", 123L);

		HasMapLong fetched = ofy().saveClearLoad(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Test(expectedExceptions = NullPointerException.class)
	public void nullKeysAreForbidden() {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put(null, 123L);

		ofy().save().entity(hml).now();
	}

	@Test
	public void nullValuesWork() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("nullvalue", null);

		HasMapLong fetched = ofy().saveClearLoad(hml);
		assert (fetched.primitives.equals(hml.primitives));
	}

	/**
	 * We should be able to store a Key<?> as the EmbedMap key using a Stringifier
	 */
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapWithKeyKey {
		@Id
		Long id;

		@Stringify(KeyStringifier.class)
		Map<Key<Trivial>, Long> primitives = new HashMap<>();
	}

	@Test
	public void keyStringifierWorks() throws Exception {
		fact().register(HasMapWithKeyKey.class);

		HasMapWithKeyKey hml = new HasMapWithKeyKey();
		hml.primitives.put(Key.create(Trivial.class, 123L), 1L);
		hml.primitives.put(Key.create(Trivial.class, 456L), 2L);

		HasMapWithKeyKey fetched = ofy().saveClearLoad(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Entity
	public static class MapWithSetIssue {
		@Id Long id;
		@Index
		private Map<String, Set<String>> mapWithSet = new HashMap<>();
	}

	@Test
	public void testMapWithSet() throws Exception {
		fact().register(MapWithSetIssue.class);

		final MapWithSetIssue mws = new MapWithSetIssue();
		mws.mapWithSet.put("key", Collections.singleton("value"));
		ofy().saveClearLoad(mws); //failure here: java.util.HashMap cannot be cast to java.util.Collection
	}

	//
	//
	//

	enum SomeEnum { ONE, TWO }

	@com.googlecode.objectify.annotation.Entity
	public static class HasEnumKeyMap {
		@Id
		Long id;
		Map<SomeEnum, Long> primitives = new HashMap<>();
	}

	@Test
	public void keyCanBeEnum() throws Exception {
		fact().register(HasEnumKeyMap.class);

		HasEnumKeyMap he = new HasEnumKeyMap();
		he.primitives.put(SomeEnum.ONE, 1L);
		he.primitives.put(SomeEnum.TWO, 2L);

		HasEnumKeyMap fetched = ofy().saveClearLoad(he);

		assert fetched.primitives.equals(he.primitives);
	}

}
