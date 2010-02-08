package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import org.testng.annotations.Test;

import javax.persistence.Id;
import javax.persistence.Transient;

/**
 */
public class TransientTests extends TestBase
{
	public static class HasTransients
	{
		@Id Long id;
		String name;
		transient int transientKeyword;
		@Transient int transientAnnotation;
	}

	/** */
	@Test
	public void testTransientFields() throws Exception
	{
		fact.register(HasTransients.class);

		Objectify ofy = fact.begin();

		HasTransients o = new HasTransients();
		o.name = "saved";
		o.transientKeyword = 42;
		o.transientAnnotation = 43;

		Key<HasTransients> k = ofy.put(o);

		o = ofy.get(k);

		assert "saved".equals(o.name);
		assert o.transientKeyword == 42;
		assert o.transientAnnotation == 0;

		Entity e = ofy.getDatastore().get(fact.typedKeyToRawKey(k));

		assert e.getProperties().size() == 2;
		assert e.getProperty("name") != null;
		assert e.getProperty("name").equals("saved");
		assert e.getProperty("transientKeyword") != null;
		assert e.getProperty("transientKeyword").equals(42L);
	}
}
