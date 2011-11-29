/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Child;
import com.googlecode.objectify.test.entity.Employee;
import com.googlecode.objectify.test.entity.NamedTrivial;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of various queries
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryTests.class.getName());

	/** */
	Trivial triv1;
	Trivial triv2;
	List<Key<Trivial>> keys;
	
	/** */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		fact.register(Trivial.class);
		
		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);
		
		List<Trivial> trivs = new ArrayList<Trivial>();
		trivs.add(this.triv1);
		trivs.add(this.triv2);
		
		TestObjectify ofy = this.fact.begin();
		Map<Key<Trivial>, Trivial> result = ofy.put().entities(trivs).now();

		this.keys = new ArrayList<Key<Trivial>>(result.keySet());
	}	
	
	/** */
	@Test
	public void testKeysOnly() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Query<Trivial> q = ofy.load().type(Trivial.class);
		
		int count = 0;
		for (Key<Trivial> k: q.keys())
		{
			assert keys.contains(k);
			count++;
		}
		
		assert count == keys.size();
		
		// Just for the hell of it, test the other methods
		assert q.count() == keys.size();
		
		q = q.limit(2);
		for (Key<Trivial> k: q.keys())
			assert keys.contains(k);
		
		Key<Trivial> first = q.keysOnly().first().key();
		assert first.equals(this.keys.get(0));
		
		q = q.offset(1);
		Key<Trivial> second = q.keysOnly().first().key();
		assert second.equals(this.keys.get(1));
	}

	/** */
	@Test
	public void testLimitAndCursorUsingIterator() throws Exception {
		// create 30 objects with someString=foo,
		// then search for limit 20 (finding cursor at 15th position)
		// then search for limit 20 using that cursor
		// then use get() and see if we get the object at cursor

		TestObjectify ofy = this.fact.begin();
		for (int i = 0; i < 30; i++) {
			ofy.put(new Trivial("foo", i));
		}

		Query<Trivial> q1 = ofy.load().type(Trivial.class).filter("someString", "foo").limit(20);
		QueryResultIterator<Trivial> i1 = q1.iterator();
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

		Query<Trivial> q2 = ofy.load().type(Trivial.class).filter("someString =", "foo").limit(20).startAt(cursor);
		QueryResultIterator<Trivial> i2 = q2.iterator();
		List<Trivial> l2 = new ArrayList<Trivial>();
		while (i2.hasNext())
		{
			Trivial trivial = i2.next();
			l2.add(trivial);
		}
		assert l2.size() == 15;

		Trivial gotten = q2.first().get();
		assert gotten.getId().equals(objectAfterCursor.getId());
	}

	/** */
	@Test
	public void testNormalSorting() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.load().type(Trivial.class).order("someString").iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testNormalReverseSorting() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.load().type(Trivial.class).order("-someString").iterator();
		
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
		TestObjectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.load().type(Trivial.class).order("id").iterator();
		
		Trivial t1 = it.next();
		Trivial t2 = it.next();
		
		assert t1.getId().equals(triv1.getId()); 
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testFiltering() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.load().type(Trivial.class).filter("someString >", triv1.getSomeString()).iterator();
			
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}

	/** */
	@Test
	public void testFilteringByNull() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv3 = new Trivial(null, 3);
		ofy.put(triv3);
		
		Iterator<Trivial> it = ofy.load().type(Trivial.class).filter("someString", null).iterator();

		assert it.hasNext();
		Trivial t3 = it.next();
		assert !it.hasNext();
		assert t3.getId().equals(triv3.getId()); 
	}

	/** */
	@Test
	public void testIdFiltering() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Iterator<Trivial> it = ofy.load().type(Trivial.class).filter("id >", triv1.getId()).iterator();
		
		Trivial t2 = it.next();
		assert !it.hasNext();
		assert t2.getId().equals(triv2.getId()); 
	}
	
	/** */
	@Test
	public void testQueryToString() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Query<Trivial> q1 = ofy.load().type(Trivial.class).filter("id >", triv1.getId());
		Query<Trivial> q2 = ofy.load().type(Trivial.class).filter("id <", triv1.getId());
		Query<Trivial> q3 = ofy.load().type(Trivial.class).filter("id >", triv1.getId()).order("-id");

		assert !q1.toString().equals(q2.toString());
		assert !q1.toString().equals(q3.toString());
	}

	/** */
	@Test
	public void testEmptySingleResult() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Query<Trivial> q = ofy.load().type(Trivial.class).filter("id", 999999);	// no such entity
		assert q.first().get() == null;
	}

	/** */
	@Test
	public void testFilteringByKeyField() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Key<Employee> bobKey = Key.create(Employee.class, "bob");
		
		Employee fred = new Employee("fred", bobKey);
		ofy.put(fred);
		
		Iterator<Employee> it = ofy.load().type(Employee.class).filter("manager", bobKey).iterator();

		assert it.hasNext();
		Employee fetched = it.next();
		assert !it.hasNext();
		assert fred.getName().equals(fetched.getName()); 
	}
	
	/** */
	@Test
	public void testFilteringByAncestor() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv = new Trivial(null, 3);
		Key<Trivial> trivKey = ofy.put(triv);
		
		Child child = new Child(trivKey, "blah");
		ofy.put(child);
		
		Iterator<Object> it = ofy.load().ancestor(trivKey).iterator();

		Object fetchedTrivial = it.next();
		assert fetchedTrivial instanceof Trivial;
		assert ((Trivial)fetchedTrivial).getId().equals(triv.getId()); 
		
		Object fetchedChild = it.next();
		assert fetchedChild instanceof Child;
		assert ((Child)fetchedChild).getId().equals(child.getId()); 
		
		assert !it.hasNext();
	}
	
	/** */
	@Test
	public void testIN() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv1 = new Trivial("foo", 3);
		Trivial triv2 = new Trivial("bar", 3);
		ofy.put(triv1);
		ofy.put(triv2);

		List<String> conditions = Arrays.asList(new String[] {"foo", "bar", "baz"});

		List<Trivial> result = ofy.load().type(Trivial.class).filter("someString in", conditions).list();
		assert result.size() == 2;
		
		long id1 = result.get(0).getId();
		long id2 = result.get(1).getId();
		
		assert id1 == triv1.getId() || id1 == triv2.getId(); 
		assert id2 == triv1.getId() || id2 == triv2.getId(); 
	}

	/** */
	@Test
	public void testINfilteringOnStringName() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		NamedTrivial triv1 = new NamedTrivial("foo", null, 3);
		NamedTrivial triv2 = new NamedTrivial("bar", null, 3);
		ofy.put(triv1);
		ofy.put(triv2);

		List<String> conditions = Arrays.asList(new String[] {"foo", "bar", "baz"});

		List<NamedTrivial> result = ofy.load().type(NamedTrivial.class).filter("name in", conditions).list();
		assert result.size() == 2;
		
		String id1 = result.get(0).getName();
		String id2 = result.get(1).getName();
		
		assert id1.equals("foo") || id1.equals("bar"); 
		assert id2.equals("foo") || id2.equals("bar"); 
	}

	/** */
	@Test
	public void testINfilteringWithKeySpecial() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Trivial triv1 = new Trivial("foo", 3);
		Key<Trivial> key1 = ofy.put(triv1);
		Set<Key<Trivial>> singleton = Collections.singleton(key1);

		List<Trivial> result = ofy.load().type(Trivial.class).filter("__key__ in", singleton).list();
		assert result.size() == 1;
		
		assert  triv1.getId().equals(result.get(0).getId()); 
	}

	/** */
	@Test
	public void testINfilteringWithKeyField() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Key<Employee> bobKey = Key.create(Employee.class, "bob");
		Employee fred = new Employee("fred", bobKey);
		
		ofy.put(fred);
		
		Set<Key<Employee>> singleton = Collections.singleton(bobKey);

		List<Employee> result = ofy.load().type(Employee.class).filter("manager in", singleton).list();
		assert result.size() == 1;
		
		assert  result.get(0).getName().equals("fred"); 
	}
	
	/** */
	@Test
	public void testCount() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		int count = ofy.load().type(Trivial.class).count();
		
		assert count == 2;
	}
}
