/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.QueryResults;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of query cursors using a setup of lots of things. Note that all the numbers are 1-based because we can't have an id of 0
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryCursorTestsBig extends TestBase {

	/** */
	private static final int MAX_ID = 30;

	/** */
	private List<Key<Trivial>> keys;
	private List<Trivial> entities;

	/** Set up the base query we use for tests */
	private Query<Trivial> query() {
		return ofy().load().type(Trivial.class).filter("someString", "foo").order("__key__");
	}

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		entities = new ArrayList<>();
		for (long i = 1; i <= MAX_ID; i++)
			entities.add(new Trivial(i, "foo", i));

		final Map<Key<Trivial>, Trivial> saved = ofy().save().entities(entities).now();
		keys = new ArrayList<>(saved.keySet());
	}

	/** */
	@Test
	void simpleOrder() throws Exception {
		final Query<Trivial> q1 = query();
		final QueryResults<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			final Trivial triv = i1.next();
			assertThat(triv).isSameInstanceAs(entities.get(which-1));
		}

		assert which == MAX_ID;
	}

	private void walkQuery(final Query<Trivial> q1, final int expectedEnd) {
		final QueryResults<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			final Cursor cursor = i1.getCursorAfter();
			assertCursorAt(cursor, which);
			i1.next();
		}

		assertThat(which).isEqualTo(expectedEnd);
	}

	/** Asserts that the next value in the cursor is the specified position */
	private void assertCursorAt(Cursor cursor, int position) {
		Trivial triv = query().startAt(cursor).first().now();
		assert triv.getSomeNumber() == position;
	}

	/** */
	@Test
	void cursorAtEveryStep() throws Exception {
		final Query<Trivial> q1 = query();
		walkQuery(q1, MAX_ID);
	}

	/** */
	@Test
	void cursorAtEveryStepWithChunk() throws Exception {
		final Query<Trivial> q1 = query().chunk(5);
		walkQuery(q1, MAX_ID);
	}

	/** */
	@Test
	void cursorAtEveryStepWithLimit() throws Exception {
		Query<Trivial> q1 = query().limit(20);
		walkQuery(q1, 20);
	}

	/** */
	@Test
	void limitAndCursorUsingIterator() throws Exception {
		// create 30 objects with someString=foo,
		// then search for limit 20 (finding cursor at 15)
		// then search using that cursor
		// then use get() and see if we get the object at cursor

		final List<Trivial> l1 = new ArrayList<>();
		Cursor cursor = null;

		final Query<Trivial> q1 = query().limit(20);
		final QueryResults<Trivial> i1 = q1.iterator();

		int which = 0;
		while (i1.hasNext()) {
			which++;
			final Trivial trivial = i1.next();
			l1.add(trivial);

			if (which == 15)
				cursor = i1.getCursorAfter();
		}

		assertThat(l1).hasSize(20);

		final Query<Trivial> q2 = query().limit(20).startAt(cursor);

		final List<Trivial> l2 = new ArrayList<>();
		for (final Trivial trivial : q2) {
			l2.add(trivial);
		}

		assertThat(l2).hasSize(15);
		assertThat(l2.get(0)).isSameInstanceAs(l1.get(15));
		assertThat(l2.get(0)).isSameInstanceAs(q2.first().now());
	}
}
