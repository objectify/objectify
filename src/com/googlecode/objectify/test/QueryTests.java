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
import com.google.appengine.api.datastore.PreparedQuery;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.OPreparedQuery;
import com.googlecode.objectify.OQuery;
import com.googlecode.objectify.Objectify;
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
	List<Key<Trivial>> keys;
	
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
		
		Objectify ofy = this.fact.begin();
		this.keys = ofy.put(trivs);
	}	
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		
		OPreparedQuery<Key<Trivial>> pq = ofy.prepareKeysOnly(q);
		
		int count = 0;
		for (Key<Trivial> k: pq.asIterable())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == keys.size();
		
		// Just for the hell of it, test the other methods
		for (Key<Trivial> k: pq.asList(FetchOptions.Builder.withLimit(1000)))
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
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.sort("someString");
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
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
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.sort("-someString");
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
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
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.sort("id");
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
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
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.filter("someString >", triv1.getSomeString());
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testIdFiltering() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.filter("id >", triv1.getId());
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
		Iterator<Trivial> it = pq.asIterable().iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testQueryToString() throws Exception
	{
		OQuery<Trivial> q1 = this.fact.createQuery(Trivial.class);
		q1.filter("id >", triv1.getId());

		OQuery<Trivial> q2 = this.fact.createQuery(Trivial.class);
		q2.filter("id <", triv1.getId());

		OQuery<Trivial> q3 = this.fact.createQuery(Trivial.class);
		q3.filter("id >", triv1.getId());
		q3.sort("-id");

		assert !q1.toString().equals(q2.toString());
		assert !q1.toString().equals(q3.toString());
	}

	/** */
	@Test
	public void testEmptySingleResult() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		q.filter("id", 999999);	// no such entity
		
		OPreparedQuery<Trivial> pq = ofy.prepare(q);
		assert pq.asSingle() == null;
	}

	/**
	 * Tests issue #3:  http://code.google.com/p/objectify-appengine/issues/detail?id=3 
	 */
	@Test
	public void testFetchOptionsWithTimeoutRetries() throws Exception
	{
		this.fact.setDatastoreTimeoutRetryCount(1);
		Objectify ofy = this.fact.begin();
		
		OQuery<Trivial> q = this.fact.createQuery(Trivial.class);
		OPreparedQuery<Key<Trivial>> pq = ofy.prepareKeysOnly(q);
		FetchOptions opts = FetchOptions.Builder.withLimit(10);
		
		// This used to throw an exception when wrapping the ArrayList in the retry wrapper
		// because we used the wrong classloader to produce the proxy.  Fixed.
    	List<Key<Trivial>> keys = pq.asList(opts);
    	
    	assert keys != null;
	}
}