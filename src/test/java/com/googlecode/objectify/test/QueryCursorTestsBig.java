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

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of query cursors using a setup of lots of things.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryCursorTestsBig extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryCursorTestsBig.class.getName());

	/** */
	private static final int COUNT = 30;

	/** */
	List<Key<Trivial>> keys;
	List<Trivial> entities;

	/** */
	@BeforeMethod
	public void setUp() {
		super.setUp();

		fact().register(Trivial.class);

		entities = new ArrayList<Trivial>();
		for (int i = 0; i < COUNT; i++)
			entities.add(new Trivial("foo", i));

		Map<Key<Trivial>, Trivial> saved = ofy().save().entities(entities).now();
		keys = new ArrayList<Key<Trivial>>(saved.keySet());
	}

	/** */
	@Test
	public void testCursorAtEveryStep() throws Exception {
		Query<Trivial> q1 = ofy().load().type(Trivial.class).filter("someString", "foo");//.chunk(10);
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int position = 0;
		while (i1.hasNext()) {
			Cursor cursor = i1.getCursor();
			assertCursorAt(cursor, position);
			i1.next();
			position++;
		}
	}

	/** Asserts that the next value in the cursor is the specified position */
	private void assertCursorAt(Cursor cursor, int position) {
		Trivial triv = ofy().load().type(Trivial.class).filter("someString",  "foo").startAt(cursor).first().get();
		assert triv.getSomeNumber() == position;
	}

	/** */
	@Test
	public void testLimitAndCursorUsingIterator() throws Exception {
		// create 30 objects with someString=foo,
		// then search for limit 20 (finding cursor at 15th position)
		// then search for limit 20 using that cursor
		// then use get() and see if we get the object at cursor

		Query<Trivial> q1 = ofy().load().type(Trivial.class).filter("someString", "foo").limit(20);
		QueryResultIterator<Trivial> i1 = q1.iterator();
		List<Trivial> l1 = new ArrayList<Trivial>();
		Cursor cursor = null;
		Trivial objectAfterCursor = null;
		int count = 1;
		while (i1.hasNext()) {
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

		Query<Trivial> q2 = ofy().load().type(Trivial.class).filter("someString =", "foo").limit(20).startAt(cursor);
		QueryResultIterator<Trivial> i2 = q2.iterator();
		List<Trivial> l2 = new ArrayList<Trivial>();
		while (i2.hasNext()) {
			Trivial trivial = i2.next();
			l2.add(trivial);
		}
		assert l2.size() == 15;

		Trivial gotten = q2.first().get();
		assert gotten.getId().equals(objectAfterCursor.getId());
	}
}
