/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Query;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Employee;
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
		Map<Key<Trivial>, Trivial> result = ofy.put(trivs);

		this.keys = new ArrayList<Key<Trivial>>(result.keySet());
	}	
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Query<Trivial> q = ofy.query(Trivial.class);
		
		int count = 0;
		for (Key<Trivial> k: q.fetchKeys())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == keys.size();
		
		// Just for the hell of it, test the other methods
		assert q.countAll() == keys.size();
		
		q.limit(2);
		for (Key<Trivial> k: q.fetchKeys())
			assert keys.contains(k);
		
		Key<Trivial> first = q.getKey();
		assert first.equals(this.keys.get(0));
		
		q.offset(1);
		Key<Trivial> second = q.getKey();
		assert second.equals(this.keys.get(1));
	}

	/** */
	@Test
	public void testLimitAndCursorUsingFetch() throws Exception {
		subtestLimitAndCursorUsingIterator(true);
	}

	/** */
	@Test
	public void testLimitAndCursorUsingIterator() throws Exception {
		subtestLimitAndCursorUsingIterator(false);
	}

	private void subtestLimitAndCursorUsingIterator(boolean useFetch)
	{
		// create 30 objects with someString=foo,
		// then search for limit 20 (finding cursor at 15th position)
		// then search for limit 20 using that cursor
		// then use get() and see if we get the object at cursor

		Objectify ofy = this.fact.begin();
		for (int i = 0; i < 30; i++) {
			ofy.put(new Trivial("foo", i));
		}

		Query<Trivial> q1 = ofy.query(Trivial.class).filter("someString", "foo");
		q1.limit(20);
		QueryResultIterator<Trivial> i1 = useFetch ? q1.fetch().iterator() : q1.iterator();
		List<Trivial> l1 = new ArrayList<Trivial>();
		Cursor cursor = null;
		Trivial objectAfterCursor = null;
		int count = 1;
		while (i1.hasNext())
		{
			Trivial trivial = i1.next();
			l1.add(trivial);
			if (count == 15) {
				cursor = i1.getCursor();
			}
			if (count == 16) {
				objectAfterCursor = trivial;
			}
			count++;
		}

		assert l1.size() == 20;

		Query<Trivial> q2 = ofy.query(Trivial.class).filter("someString =", "foo");
		q2.limit(20).cursor(cursor);
		QueryResultIterator<Trivial> i2 = useFetch ? q2.fetch().iterator() : q2.iterator();
		List<Trivial> l2 = new ArrayList<Trivial>();
		while (i2.hasNext())
		{
			Trivial trivial = i2.next();
			l2.add(trivial);
		}
		assert l2.size() == 15;

		Trivial gotten = q2.get();
		assert gotten.getId().equals(objectAfterCursor.getId());
	}

	/** */
	@Test
	public void testNormalSorting() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).order("someString").iterator();
		
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
		Iterator<Trivial> it = ofy.query(Trivial.class).order("-someString").iterator();
		
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
		Iterator<Trivial> it = ofy.query(Trivial.class).order("id").iterator();
		
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
		Iterator<Trivial> it = ofy.query(Trivial.class).filter("someString >", triv1.getSomeString()).iterator();
			
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testFilteringByNull() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Trivial triv3 = new Trivial(null, 3);
		ofy.put(triv3);
		
		Iterator<Trivial> it = ofy.query(Trivial.class).filter("someString", null).iterator();

		assert it.hasNext();
		Trivial t3 = it.next();
		assert !it.hasNext();
		assert t3.getId().equals(triv3.getId()); 
	}

	/** */
	@Test
	public void testIdFiltering() throws Exception
	{
		Objectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.query(Trivial.class).filter("id >", triv1.getId()).iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testQueryToString() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Query<Trivial> q1 = ofy.query(Trivial.class).filter("id >", triv1.getId());
		Query<Trivial> q2 = ofy.query(Trivial.class).filter("id <", triv1.getId());
		Query<Trivial> q3 = ofy.query(Trivial.class).filter("id >", triv1.getId()).order("-id");

		assert !q1.toString().equals(q2.toString());
		assert !q1.toString().equals(q3.toString());
	}

	/** */
	@Test
	public void testEmptySingleResult() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Query<Trivial> q = ofy.query(Trivial.class).filter("id", 999999);	// no such entity
		assert q.get() == null;
	}

	/**
	 * Tests issue #3:  http://code.google.com/p/objectify-appengine/issues/detail?id=3 
	 */
	@Test
	public void testFetchOptionsWithTimeoutRetries() throws Exception
	{
		this.fact.setDatastoreTimeoutRetryCount(1);
		Objectify ofy = this.fact.begin();
		
		// This used to throw an exception when wrapping the ArrayList in the retry wrapper
		// because we used the wrong classloader to produce the proxy.  Fixed.
    	Iterable<Key<Trivial>> keys = ofy.query(Trivial.class).limit(10).fetchKeys();
    	
    	assert keys != null;
	}

	/** */
	@Test
	public void testFilteringByKeyField() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Key<Employee> bobKey = new Key<Employee>(Employee.class, "bob");
		
		Employee fred = new Employee("fred", bobKey);
		ofy.put(fred);
		
		Iterator<Employee> it = ofy.query(Employee.class).filter("manager", bobKey).iterator();

		assert it.hasNext();
		Employee fetched = it.next();
		assert !it.hasNext();
		assert fred.getName().equals(fetched.getName()); 
	}
	
	/** This is expected to fail without a kind: see http://code.google.com/p/googleappengine/issues/detail?id=2196 */
	@Test
	public void testFilteringByAncestor() throws Exception
	{
		Objectify ofy = this.fact.begin();
		
		Trivial triv = new Trivial(null, 3);
		Key<Trivial> trivKey = ofy.put(triv);
		
		Child child = new Child(trivKey, "blah");
		ofy.put(child);
		
		Iterator<Child> it = ofy.<Child>query().ancestor(trivKey).iterator();

		assert it.hasNext();	// fails due to known GAE SDK bug
		Child fetched = it.next();
		assert !it.hasNext();
		assert child.getId().equals(fetched.getId()); 
	}
}
