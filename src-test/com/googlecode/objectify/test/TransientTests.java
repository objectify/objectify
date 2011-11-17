package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 */
public class TransientTests extends TestBase
{
    @com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasTransients
	{
		@Id Long id;
		String name;
		transient int transientKeyword;
		@Ignore int transientAnnotation;
	}

	/** */
	@Test
	public void testTransientFields() throws Exception
	{
		fact.register(HasTransients.class);

		TestObjectify ofy = fact.begin();

		HasTransients o = new HasTransients();
		o.name = "saved";
		o.transientKeyword = 42;
		o.transientAnnotation = 43;

		Key<HasTransients> k = ofy.put(o);

		o = ofy.get(k);

		assert "saved".equals(o.name);
		assert o.transientKeyword == 42;
		assert o.transientAnnotation == 0;	// fails with caching objectify, this is ok

		Entity e = ds().get(null, k.getRaw());

		assert e.getProperties().size() == 2;
		assert e.getProperty("name") != null;
		assert e.getProperty("name").equals("saved");
		assert e.getProperty("transientKeyword") != null;
		assert ((Number)e.getProperty("transientKeyword")).intValue() == 42;
	}
}
