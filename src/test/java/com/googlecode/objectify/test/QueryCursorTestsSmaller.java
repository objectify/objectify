/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.QueryResults;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * Trying to boil down a bug in cursoring to the smallest possible case
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryCursorTestsSmaller extends TestBase {

	/** */
	@Test
	void simplerVersionOfCursorEnd() throws Exception {
		factory().register(Trivial.class);
		ofy().save().entity(new Trivial("foo1", 1)).now();

		final Query<Trivial> q = ofy().load().type(Trivial.class).hybrid(true);

		final QueryResults<Trivial> from0 = q.iterator();
		from0.next();

		assertThat(from0.hasNext()).isFalse();

		// We should be at end
		final QueryResults<Trivial> from2 = q.startAt(from0.getCursorAfter()).iterator();
		assertThat(from2.hasNext()).isFalse();

		// Try that again just to be sure
		final QueryResults<Trivial> from2Again = q.startAt(from2.getCursorAfter()).iterator();
		assertThat(from2Again.hasNext()).isFalse();
	}
}
