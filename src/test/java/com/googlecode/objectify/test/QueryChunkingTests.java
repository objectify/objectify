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
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.Map;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Lowish-level tests for chunking behavior of queries.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryChunkingTests extends TestBase
{
	private static final int BATCH_SIZE = 2;

	QueryResultIterator<Key<Trivial>> keysIt;
	Map<Key<Trivial>, Trivial> values;

	LoadEngine loadEngine;

	/** */
	@BeforeMethod
	public void setUpExtra() {
		fact().register(Trivial.class);

		values = Maps.newHashMap();

		for (int i=10; i<15; i++) {
			Trivial triv = new Trivial((long)i, "str"+i, i);
			Key<Trivial> key = ofy().save().entity(triv).now();
			values.put(key, triv);
		}

		keysIt = ofy().load().type(Trivial.class).chunk(BATCH_SIZE).keys().iterator();

		loadEngine = mock(LoadEngine.class);

		for (Map.Entry<Key<Trivial>, Trivial> entry: values.entrySet())
			when(loadEngine.load(entry.getKey())).thenReturn(new ResultNow<>(entry.getValue()));
	}

	/** */
	@Test
	public void testChunkIterator() throws Exception {
		ChunkIterator<Trivial> chunkIt = new ChunkIterator<>(keysIt, BATCH_SIZE, loadEngine);

		Chunk<Trivial> chunk;
		ResultWithCursor<Trivial> rc;

		// First chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assert rc.getResult().getId() == 10;
		assertCursorGetsId(rc.getCursor(), 10);
		assert rc.getOffset() == 0;

		rc = chunk.next();
		assert rc.getResult().getId() == 11;
		assertCursorGetsId(rc.getCursor(), 10);
		assert rc.getOffset() == 1;


		// Second chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assert rc.getResult().getId() == 12;
		assertCursorGetsId(rc.getCursor(), 12);
		assert rc.getOffset() == 0;

		rc = chunk.next();
		assert rc.getResult().getId() == 13;
		assertCursorGetsId(rc.getCursor(), 12);
		assert rc.getOffset() == 1;


		// Third (abbreviated) chunk
		chunk = chunkIt.next();

		rc = chunk.next();
		assert rc.getResult().getId() == 14;
		assertCursorGetsId(rc.getCursor(), 14);
		assert rc.getOffset() == 0;

	}

	/**
	 * Assert that fetching from the cursor gets a trivial with the specified id as the first item.
	 */
	private void assertCursorGetsId(Cursor cursor, long trivId) {
		assert ofy().load().type(Trivial.class).startAt(cursor).first().now().getId() == trivId;
	}
}
