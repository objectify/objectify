/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.googlecode.objectify.ObPreparedQuery;
import com.googlecode.objectify.ObQuery;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of various queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(QueryTests.class);

	/** */
	Trivial triv1;
	Trivial triv2;
	List<Key> keys;
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);
		
		List<Trivial> trivs = new ArrayList<Trivial>();
		trivs.add(this.triv1);
		trivs.add(this.triv2);
		
		Objectify ofy = ObjectifyFactory.begin();
		this.keys = ofy.put(trivs);
	}	
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.keysOnly();
		
		ObPreparedQuery<Key> pq = ofy.prepare(q);
		
		int count = 0;
		for (Key k: pq.asIterable())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == keys.size();
		
		// Just for the hell of it, test the other methods
		for (Key k: pq.asList(FetchOptions.Builder.withLimit(1000)))
			assert keys.contains(k);
		
		assert pq.count() == keys.size();
		
		try
		{
			pq.asSingle();
			assert false: "Should not be able to asSingle() when there are multiple results";
		}
		catch (PreparedQuery.TooManyResultsException ex) {}
	}

	/** */
	@Test
	public void testNormalSorting() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.sort("someString");
		
		ObPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testNormalReverseSorting() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.sort("-someString");
		
		ObPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		// t2 first
		Trivial t2 = it.next();
		Trivial t1 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** Unfortunately we can only test one way without custom index file */
	@Test
	public void testIdSorting() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.sort("id");
		
		ObPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testFiltering() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.filter("someString >", triv1.getSomeString());
		
		ObPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testIdFiltering() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		ObQuery q = ObjectifyFactory.createQuery(Trivial.class);
		q.filter("id >", triv1.getId());
		
		ObPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}
}