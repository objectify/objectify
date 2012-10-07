/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Normally we run all tests with caching enabled.  This lets us mix cached
 * and uncached in batches to see what happens.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CachingTests.class.getName());

	/** */
	@Entity
	static class Uncached
	{
		@Id Long id;
		String stuff;
	}

	/** */
	@Entity
	@Cache
	static class Cached
	{
		@Id Long id;
		String stuff;
	}

	/**
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();

		this.fact.register(Uncached.class);
		this.fact.register(Cached.class);
	}

	/** */
	@Test
	public void testHeterogeneousBatch() throws Exception
	{
		Uncached un1 = new Uncached();
		un1.stuff = "un1 stuff";

		Uncached un2 = new Uncached();
		un2.stuff = "un2 stuff";

		Cached ca1 = new Cached();
		ca1.stuff = "ca1 stuff";

		Cached ca2 = new Cached();
		ca2.stuff = "ca2 stuff";

		List<Object> entities = new ArrayList<Object>();
		entities.add(un1);
		entities.add(ca1);
		entities.add(un2);
		entities.add(ca2);

		TestObjectify ofy = this.fact.begin();

		Map<Key<Object>, Object> keys = ofy.save().entities(entities).now();
		ofy.clear();
		Map<Key<Object>, Object> fetched = ofy.load().keys(keys.keySet());

		assert fetched.size() == 4;
		assert fetched.containsKey(Key.create(un1));
		assert fetched.containsKey(Key.create(un2));
		assert fetched.containsKey(Key.create(ca1));
		assert fetched.containsKey(Key.create(ca2));
	}
}