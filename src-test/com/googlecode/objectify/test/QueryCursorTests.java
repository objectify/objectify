/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of query cursors
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryCursorTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryCursorTests.class.getName());

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
		Map<Key<Trivial>, Trivial> result = ofy.save().entities(trivs).now();

		this.keys = new ArrayList<Key<Trivial>>(result.keySet());
	}	
	
	/** */
	@Test
	public void testCursorEnd() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		Query<Trivial> q = ofy.load().type(Trivial.class);
		QueryResultIterator<Trivial> it = q.limit(1).iterator();
		
		assert it.hasNext();
		Trivial t1 = it.next();
		assert t1.getId().equals(triv1.getId());
		assert !it.hasNext();
		
		Cursor cursor = it.getCursor();
		assert cursor != null;
		
		it = q.startAt(cursor).limit(1).iterator();
		
		assert it.hasNext();
		Trivial t2 = it.next();
		assert t2.getId().equals(triv2.getId());
		assert !it.hasNext();

		// We should be at end
		cursor = it.getCursor();
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();
		
		// Try that again just to be sure
		cursor = it.getCursor();
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();
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
}
