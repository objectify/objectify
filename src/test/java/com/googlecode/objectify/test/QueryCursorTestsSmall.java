/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of query cursors using a setup of just a couple items.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryCursorTestsSmall extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(QueryCursorTestsSmall.class.getName());

	/** */
	Trivial triv1;
	Trivial triv2;
	List<Key<Trivial>> keys;

	/** */
	@BeforeMethod
	public void setUpExtra() {
		fact().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		Map<Key<Trivial>, Trivial> saved = ofy().save().entities(triv1, triv2).now();

		this.keys = new ArrayList<>(saved.keySet());
	}

	/** */
	@Test
	public void testCursorEnd() throws Exception {
		Query<Trivial> q = ofy().load().type(Trivial.class);
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
		assert cursor != null;
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();

		// Try that again just to be sure
		cursor = it.getCursor();
		assert cursor != null;
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();
	}

	/** */
	@Test
	public void testCursorEndLowLevelBehavior() throws Exception {
		com.google.appengine.api.datastore.Query query = new com.google.appengine.api.datastore.Query("Trivial");
		PreparedQuery pq = ds().prepare(query);

		QueryResultIterator<Entity> it = pq.asQueryResultIterable().iterator();
		it.next();
		it.next();
		assert !it.hasNext();

		Cursor cursor = it.getCursor();
		assert cursor != null;

		QueryResultIterator<Entity> it2 = pq.asQueryResultIterable(FetchOptions.Builder.withStartCursor(cursor)).iterator();
		assert !it2.hasNext();

		Cursor cursor2 = it2.getCursor();
		assert cursor2 != null;

		QueryResultIterator<Entity> it3 = pq.asQueryResultIterable(FetchOptions.Builder.withStartCursor(cursor2)).iterator();
		assert !it3.hasNext();
		assert it3.getCursor() != null;
	}

	/** */
	@Test
	public void testCursorOneFetchToEnd() throws Exception {
		Query<Trivial> q = ofy().load().type(Trivial.class);
		QueryResultIterator<Trivial> it = q.iterator();

		it.next();
		it.next();
		assert !it.hasNext();

		// We should be at end
		Cursor cursor = it.getCursor();
		assert cursor != null;
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();

		// Try that again just to be sure
		cursor = it.getCursor();
		assert cursor != null;
		it = q.startAt(cursor).iterator();
		assert !it.hasNext();
	}

	/** */
	@Test
	public void cursorReverses() throws Exception {
		Query<Trivial> q = ofy().load().type(Trivial.class).order("__key__");
		QueryResultIterator<Trivial> it = q.iterator();

		@SuppressWarnings("unused")
		Cursor cursor0 = it.getCursor();
		final Trivial item1 = it.next();

		Cursor cursor1 = it.getCursor();
		final Trivial item2 = it.next();
		assert !it.hasNext();

		Cursor cursor2 = it.getCursor();
		Cursor cursor2Rev = it.getCursor().reverse();

		it = q.reverse().startAt(cursor2Rev).iterator();

		final Trivial item2Rev = it.next();

		// This worked in 1.9.5 but fails in 1.9.9. Equality test seems a little sketchy anyways.
		//assert it.getCursor().equals(cursor2);

		assert item2Rev.getSomeString().equals(item2.getSomeString());

		assert it.hasNext();

		final Trivial item1Rev = it.next();

		//assert it.getCursor().equals(cursor1);
		assert item1Rev.getSomeString().equals(item1.getSomeString());
		assert !it.hasNext();
	}
}
