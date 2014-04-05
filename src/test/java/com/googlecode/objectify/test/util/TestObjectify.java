package com.googlecode.objectify.test.util;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.ObjectifyImpl;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;

/**
 * Adds some convenience methods.
 */
public class TestObjectify extends ObjectifyImpl<TestObjectify>
{
	/** */
	public TestObjectify(ObjectifyFactory fact) {
		super(fact);
	}

	/** Utility methods that puts, clears the session, and immediately gets an entity */
	public <T> T saveClearLoad(T saveMe) {
		Key<T> key = save().entity(saveMe).now();

		try {
			Entity ent = ds().get(null, key.getRaw());
			System.out.println(ent);
		}
		catch (EntityNotFoundException e) { throw new RuntimeException(e); }

		clear();

		return load().key(key).now();
	}

}
