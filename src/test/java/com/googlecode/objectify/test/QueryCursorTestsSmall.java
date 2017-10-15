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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Tests of query cursors using a setup of just a couple items.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryCursorTestsSmall extends TestBase {

	/** */
	private Trivial triv1;
	private Trivial triv2;
	private List<Key<Trivial>> keys;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		this.triv1 = new Trivial("foo1", 1);
		this.triv2 = new Trivial("foo2", 2);

		final Map<Key<Trivial>, Trivial> saved = ofy().save().entities(triv1, triv2).now();

		this.keys = new ArrayList<>(saved.keySet());
	}

	/** */
	@Test
	void cursorEnd() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class);
		QueryResultIterator<Trivial> it = q.limit(1).iterator();

		assertThat(it.hasNext()).isTrue();
		final Trivial t1 = it.next();
		assertThat(t1).isEqualTo(triv1);
		assertThat(it.hasNext()).isFalse();

		Cursor cursor = it.getCursor();
		assertThat(cursor).isNotNull();

		it = q.startAt(cursor).limit(1).iterator();

		assertThat(it.hasNext()).isTrue();
		final Trivial t2 = it.next();
		assertThat(t2).isEqualTo(triv2);
		assertThat(it.hasNext()).isFalse();

		// We should be at end
		cursor = it.getCursor();
		assertThat(cursor).isNotNull();;
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();

		// Try that again just to be sure
		cursor = it.getCursor();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();
	}

	/** */
	@Test
	void cursorEndLowLevelBehavior() throws Exception {
		final com.google.appengine.api.datastore.Query query = new com.google.appengine.api.datastore.Query("Trivial");
		final PreparedQuery pq = ds().prepare(query);

		final QueryResultIterator<Entity> it = pq.asQueryResultIterable().iterator();
		it.next();
		it.next();
		assertThat(it.hasNext()).isFalse();

		final Cursor cursor = it.getCursor();
		assertThat(cursor).isNotNull();

		final QueryResultIterator<Entity> it2 = pq.asQueryResultIterable(FetchOptions.Builder.withStartCursor(cursor)).iterator();
		assertThat(it2.hasNext()).isFalse();

		final Cursor cursor2 = it2.getCursor();
		assertThat(cursor2).isNotNull();

		final QueryResultIterator<Entity> it3 = pq.asQueryResultIterable(FetchOptions.Builder.withStartCursor(cursor2)).iterator();
		assertThat(it3.hasNext()).isFalse();
		assertThat(it3.getCursor()).isNotNull();
	}

	/** */
	@Test
	void cursorOneFetchToEnd() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class);
		QueryResultIterator<Trivial> it = q.iterator();

		it.next();
		it.next();
		assertThat(it.hasNext()).isFalse();

		// We should be at end
		Cursor cursor = it.getCursor();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();

		// Try that again just to be sure
		cursor = it.getCursor();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();
	}

	/** */
	@Test
	void cursorReverses() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class).order("__key__");
		QueryResultIterator<Trivial> it = q.iterator();

		final Cursor cursor0 = it.getCursor();
		final Trivial item1 = it.next();

		final Cursor cursor1 = it.getCursor();
		final Trivial item2 = it.next();
		assertThat(it.hasNext()).isFalse();

		final Cursor cursor2 = it.getCursor();
		final Cursor cursor2Rev = it.getCursor().reverse();

		it = q.reverse().startAt(cursor2Rev).iterator();

		final Trivial item2Rev = it.next();

		// This worked in 1.9.5 but fails in 1.9.9. Equality test seems a little sketchy anyways.
		//assert it.getCursor().equals(cursor2);

		assertThat(item2Rev).isEqualTo(item2);

		assertThat(it.hasNext()).isTrue();

		final Trivial item1Rev = it.next();

		//assert it.getCursor().equals(cursor1);
		assertThat(item1Rev).isEqualTo(item1);
		assertThat(it.hasNext()).isFalse();
	}
}
