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
 * Tests of query cursors using a setup of lots of things. Note that all the numbers are 1-based because we can't have an id of 0
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryCursorTestsBig extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryCursorTestsBig.class.getName());

	/** */
	private static final int MAX_ID = 30;

	/** */
	List<Key<Trivial>> keys;
	List<Trivial> entities;

	/** Set up the base query we use for tests */
	private Query<Trivial> query() {
		return ofy().load().type(Trivial.class).filter("someString", "foo").order("__key__");
	}

	/** */
	@BeforeMethod
	public void setUp() {
		super.setUp();

		fact().register(Trivial.class);

		entities = new ArrayList<>();
		for (long i = 1; i <= MAX_ID; i++)
			entities.add(new Trivial(i, "foo", i));

		Map<Key<Trivial>, Trivial> saved = ofy().save().entities(entities).now();
		keys = new ArrayList<>(saved.keySet());
	}

	/** */
	@Test
	public void simpleOrder() throws Exception {
		Query<Trivial> q1 = query();
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			Trivial triv = i1.next();
			assert triv == entities.get(which-1);
		}

		assert which == MAX_ID;
	}

	/** */
	@Test
	public void testCursorAtEveryStep() throws Exception {
		Query<Trivial> q1 = query();
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			Cursor cursor = i1.getCursor();
			assertCursorAt(cursor, which);
			i1.next();
		}

		assert which == MAX_ID;
	}

	/** */
	@Test
	public void testCursorAtEveryStepWithChunk() throws Exception {
		Query<Trivial> q1 = query().chunk(5);
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			Cursor cursor = i1.getCursor();
			assertCursorAt(cursor, which);
			i1.next();
		}

		assert which == MAX_ID;
	}

	/** */
	@Test
	public void testCursorAtEveryStepWithLimit() throws Exception {
		Query<Trivial> q1 = query().limit(20);
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			Cursor cursor = i1.getCursor();
			assertCursorAt(cursor, which);
			i1.next();
		}

		assert which == 20;
	}

	/** Asserts that the next value in the cursor is the specified position */
	private void assertCursorAt(Cursor cursor, int position) {
		Trivial triv = query().startAt(cursor).first().now();
		assert triv.getSomeNumber() == position;
	}

	/** */
	@Test
	public void testLimitAndCursorUsingIterator() throws Exception {
		// create 30 objects with someString=foo,
		// then search for limit 20 (finding cursor at 15)
		// then search using that cursor
		// then use get() and see if we get the object at cursor

		List<Trivial> l1 = new ArrayList<>();
		Cursor cursor = null;

		Query<Trivial> q1 = query().limit(20);
		QueryResultIterator<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			Trivial trivial = i1.next();
			l1.add(trivial);

			if (which == 15)
				cursor = i1.getCursor();
		}

		assert l1.size() == 20;

		List<Trivial> l2 = new ArrayList<>();

		Query<Trivial> q2 = query().limit(20).startAt(cursor);
		QueryResultIterator<Trivial> i2 = q2.iterator();

		while (i2.hasNext()) {
			Trivial trivial = i2.next();
			l2.add(trivial);
		}
		assert l2.size() == 15;

		assert l2.get(0) == l1.get(15);
		assert l2.get(0) == q2.first().now();
	}
}
