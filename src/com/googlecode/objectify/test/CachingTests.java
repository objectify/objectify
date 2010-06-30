/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Trivial;

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
	static class Uncached
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
	}
	
	/** */
	@Test
	public void testHeterogeneousBatch() throws Exception
	{
		Uncached un1 = new Uncached();
		un1.stuff = "un1 stuff";
		
		Uncached un2 = new Uncached();
		un2.stuff = "un2 stuff";

		Trivial triv1 = new Trivial("foo1", 5);
		Trivial triv2 = new Trivial("foo2", 6);

		List<Object> entities = new ArrayList<Object>();
		entities.add(un1);
		entities.add(triv1);
		entities.add(un2);
		entities.add(triv2);
		
		Objectify ofy = this.fact.begin();
		
		Map<Key<Object>, Object> keys = ofy.put(entities);
		
		Map<Key<Object>, Object> fetched = ofy.get(keys.keySet());
		
		assert fetched.size() == 4;
		assert fetched.containsKey(this.fact.getKey(un1));
		assert fetched.containsKey(this.fact.getKey(un2));
		assert fetched.containsKey(this.fact.getKey(triv1));
		assert fetched.containsKey(this.fact.getKey(triv2));
	}
}