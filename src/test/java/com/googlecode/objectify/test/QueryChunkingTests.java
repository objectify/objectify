/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.Chunk;
import com.googlecode.objectify.impl.ChunkIterator;
import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.ResultWithCursor;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.ResultNow;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.mockito.Mockito.when;

/**
 * Lowish-level tests for chunking behavior of queries.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class QueryChunkingTests extends TestBase {
	private static final int BATCH_SIZE = 2;

	private QueryResultIterator<Key<Trivial>> keysIt;
	private Map<Key<Trivial>, Trivial> values;

	@Mock
	private LoadEngine loadEngine;

	/** */
	@BeforeEach
	void setUpExtra() {
		factory().register(Trivial.class);

		values = Maps.newHashMap();

		for (int i=10; i<15; i++) {
			final Trivial triv = new Trivial((long)i, "str"+i, i);
			final Key<Trivial> key = ofy().save().entity(triv).now();
			values.put(key, triv);
		}

		keysIt = ofy().load().type(Trivial.class).chunk(BATCH_SIZE).keys().iterator();

		for (final Map.Entry<Key<Trivial>, Trivial> entry: values.entrySet())
			when(loadEngine.load(entry.getKey())).thenReturn(new ResultNow<>(entry.getValue()));
	}

	/** */
	@Test
	void testChunkIterator() throws Exception {
		final ChunkIterator<Trivial> chunkIt = new ChunkIterator<>(keysIt, BATCH_SIZE, loadEngine);

		Chunk<Trivial> chunk;
		ResultWithCursor<Trivial> rc;

		// First chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assertThat(rc.getResult().getId()).isEqualTo(10);
		assertCursorGetsId(rc.getCursor(), 10);
		assertThat(rc.getOffset()).isEqualTo(0);

		rc = chunk.next();
		assertThat(rc.getResult().getId()).isEqualTo(11);
		assertCursorGetsId(rc.getCursor(), 10);
		assertThat(rc.getOffset()).isEqualTo(1);

		// Second chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assertThat(rc.getResult().getId()).isEqualTo(12);
		assertCursorGetsId(rc.getCursor(), 12);
		assertThat(rc.getOffset()).isEqualTo(0);

		rc = chunk.next();
		assertThat(rc.getResult().getId()).isEqualTo(13);
		assertCursorGetsId(rc.getCursor(), 12);
		assertThat(rc.getOffset()).isEqualTo(1);

		// Third (abbreviated) chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assertThat(rc.getResult().getId()).isEqualTo(14);
		assertCursorGetsId(rc.getCursor(), 14);
		assertThat(rc.getOffset()).isEqualTo(0);
	}

	/**
	 * Assert that fetching from the cursor gets a trivial with the specified id as the first item.
	 */
	private void assertCursorGetsId(Cursor cursor, long trivId) {
		assertThat(ofy().load().type(Trivial.class).startAt(cursor).first().now().getId()).isEqualTo(trivId);
	}
}
