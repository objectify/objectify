package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.TranslateException;
import com.googlecode.objectify.annotation.EmbedMap;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Test of expando-type maps that hold primitve values
 */
public class EmbedMapTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapLong {
		@Id
		Long id;

		@EmbedMap
		Map<String, Long> primitives = new HashMap<String, Long>();
	}

	@Test
	public void testPrimitivesMap() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("one", 1L);
		hml.primitives.put("two", 2L);

		HasMapLong fetched = this.putClearGet(hml);

		assert fetched.primitives.equals(hml.primitives);
	}

	@Test
	public void testDotsForbidden() {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("illegal.name", 123L);

		try {
			ofy().save().entity(hml).now();
			assert false;
		}
		catch (TranslateException e) {
			// expected
		}
	}

	@Test
	public void testNullKeysForbidden() {
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
	public void testNullValuesWork() throws Exception {
		fact().register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("nullvalue", null);

		HasMapLong fetched = this.putClearGet(hml);
		assert (fetched.primitives.equals(hml.primitives));
	}

	@com.googlecode.objectify.annotation.Entity
	public static class MissingEmbedMapAnnotation {
		@Id
		Long id;

		Map<String, Long> primitives = new HashMap<String, Long>();
	}

	/**
	 * We shouldn't be able to register without the @EmbedMap annotation; secures future compatiblity
	 */
	@Test(expectedExceptions=IllegalStateException.class)
	public void testMissingEmbedMapAnnotation() throws Exception {
		fact().register(MissingEmbedMapAnnotation.class);
	}


}
