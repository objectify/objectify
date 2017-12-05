/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Cursor;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityQuery;
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
		QueryResults<Trivial> it = q.limit(1).iterator();

		assertThat(it.hasNext()).isTrue();
		final Trivial t1 = it.next();
		assertThat(t1).isEqualTo(triv1);
		assertThat(it.hasNext()).isFalse();

		Cursor cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();

		it = q.startAt(cursor).limit(1).iterator();

		assertThat(it.hasNext()).isTrue();
		final Trivial t2 = it.next();
		assertThat(t2).isEqualTo(triv2);
		assertThat(it.hasNext()).isFalse();

		// We should be at end
		cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();;
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();

		// Try that again just to be sure
		cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();
	}

	/** */
	@Test
	void cursorEndLowLevelBehavior() throws Exception {
		final com.google.cloud.datastore.Query<Entity> query = com.google.cloud.datastore.Query.newEntityQueryBuilder().setKind("Trivial").build();

		final QueryResults<Entity> it = datastore().run(query);
		it.next();
		it.next();
		assertThat(it.hasNext()).isFalse();

		final Cursor cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();

		final EntityQuery query2 = com.google.cloud.datastore.Query.newEntityQueryBuilder().setKind("Trivial").setStartCursor(cursor).build();
		final QueryResults<Entity> it2 = datastore().run(query2);
		assertThat(it2.hasNext()).isFalse();

		final Cursor cursor2 = it2.getCursorAfter();
		assertThat(cursor2).isNotNull();

		final EntityQuery query3 = com.google.cloud.datastore.Query.newEntityQueryBuilder().setKind("Trivial").setStartCursor(cursor2).build();
		final QueryResults<Entity> it3 = datastore().run(query3);
		assertThat(it3.hasNext()).isFalse();
		assertThat(it3.getCursorAfter()).isNotNull();
	}

	/** */
	@Test
	void cursorOneFetchToEnd() throws Exception {
		final Query<Trivial> q = ofy().load().type(Trivial.class);
		QueryResults<Trivial> it = q.iterator();

		it.next();
		it.next();
		assertThat(it.hasNext()).isFalse();

		// We should be at end
		Cursor cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();

		// Try that again just to be sure
		cursor = it.getCursorAfter();
		assertThat(cursor).isNotNull();
		it = q.startAt(cursor).iterator();
		assertThat(it.hasNext()).isFalse();
	}
}
