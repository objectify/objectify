package com.googlecode.objectify.test;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import com.googlecode.objectify.TranslateException;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Test of expando-type maps that hold primitve values
 */
public class MapTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	public static class HasMapLong
	{
		@Id
		Long id;
		Map<String, Long> primitives = new HashMap<String, Long>();
	}

	@Test
	public void testPrimitivesMap() throws Exception
	{
		this.fact.register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("one", 1L);
		hml.primitives.put("two", 2L);

		HasMapLong fetched = this.putClearGet(hml);
		
		assert fetched.primitives.equals(hml.primitives);
	}

	@Test
	public void testDotsForbidden()
	{
		this.fact.register(HasMapLong.class);
		TestObjectify ofy = this.fact.begin();
		
		HasMapLong hml = new HasMapLong();
		hml.primitives.put("illegal.name", 123L);

		try {
			ofy.save().entity(hml).now();
			assert false;
		}
		catch (TranslateException e) {
			// expected
		}
	}

	@Test
	public void testNullKeysForbidden()
	{
		this.fact.register(HasMapLong.class);
		TestObjectify ofy = this.fact.begin();

		HasMapLong hml = new HasMapLong();
		hml.primitives.put(null, 123L);

		try {
			ofy.save().entity(hml).now();
			assert false;
		}
		catch (TranslateException e) {
			// expected
		}
	}

	@Test
	public void testNullValuesWork() throws Exception
	{
		this.fact.register(HasMapLong.class);

		HasMapLong hml = new HasMapLong();
		hml.primitives.put("nullvalue", null);

		HasMapLong fetched = this.putClearGet(hml);
		assert (fetched.primitives.equals(hml.primitives));
	}
}
