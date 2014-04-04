package com.googlecode.objectify.test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.TranslateException;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

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

		Map<String, Long> primitives = new HashMap<String, Long>();
	}

	@Test
	public void valuesCanBePrimitives() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("one", 1L);
		hml.primitives.put("two", 2L);

		HasMapLong fetched = ofy().putClearGet(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Test
	public void dotsAreAllowed() {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("legal.name", 123L);

		HasMapLong fetched = ofy().putClearGet(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Test
	public void nullKeysAreForbidden() {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put(null, 123L);

		try {
			ofy().save().entity(hml).now();
			assert false;
		}
		catch (TranslateException e) {
			// expected
		}
	}

	@Test
	public void nullValuesWork() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("nullvalue", null);

		HasMapLong fetched = ofy().putClearGet(hml);
		assert (fetched.primitives.equals(hml.primitives));
	}

	/**
	 * We should be able to store a Key<?> as the EmbedMap key
	 */
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapWithKeyKey {
		@Id
		Long id;

		Map<Key<Trivial>, Long> primitives = new HashMap<>();
	}

	@Test
	public void keyKeyStoresOk() throws Exception {
		fact().register(HasMapWithKeyKey.class);

		HasMapWithKeyKey hml = new HasMapWithKeyKey();
		hml.primitives.put(Key.create(Trivial.class, 123L), 1L);
		hml.primitives.put(Key.create(Trivial.class, 456L), 2L);

		HasMapWithKeyKey fetched = ofy().putClearGet(hml);

		assert fetched.primitives.equals(hml.primitives);
	}
}
