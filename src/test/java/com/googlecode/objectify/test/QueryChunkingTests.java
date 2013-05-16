/*
 */

package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.impl.engine.QueryResultBatch;
import com.googlecode.objectify.impl.engine.QueryResultBatchIterator;
import com.googlecode.objectify.impl.engine.QueryResultStreamIterator;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.ResultNow;

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
	public void setUp() {
		super.setUp();

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
			when(loadEngine.load(entry.getKey())).thenReturn(new ResultNow<Trivial>(entry.getValue()));
	}

	/** */
	@Test
	public void testQueryResultBatchIterator() throws Exception {
		QueryResultBatchIterator<Trivial> batchIt = new QueryResultBatchIterator<Trivial>(keysIt, BATCH_SIZE, loadEngine);

		QueryResultBatch<Trivial> batch0 = batchIt.next();
		assert Iterables.get(batch0.getResult(), 0).getId() == 10;
		assert Iterables.get(batch0.getResult(), 1).getId() == 11;
		assertCursorGetsId(batch0.getCursor(), 10);

		QueryResultBatch<Trivial> batch1 = batchIt.next();
		assert Iterables.get(batch1.getResult(), 0).getId() == 12;
		assert Iterables.get(batch1.getResult(), 1).getId() == 13;
		assertCursorGetsId(batch1.getCursor(), 12);

		QueryResultBatch<Trivial> batch2 = batchIt.next();
		assert Iterables.get(batch2.getResult(), 0).getId() == 14;
		assertCursorGetsId(batch2.getCursor(), 14);
	}

	/** */
	@Test
	public void testQueryResultStreamIterator() throws Exception {
		QueryResultStreamIterator<Trivial> streamIt = new QueryResultStreamIterator<Trivial>(keysIt, BATCH_SIZE, loadEngine);

		assert streamIt.hasNext();
		assertCursorGetsId(streamIt.getBaseCursor(), 10);
		assert streamIt.getOffset() == 0;
		assert streamIt.next().getId() == 10;	// next
		assertCursorGetsId(streamIt.getBaseCursor(), 10);
		assert streamIt.getOffset() == 1;

		assert streamIt.hasNext();
		assertCursorGetsId(streamIt.getBaseCursor(), 10);
		assert streamIt.getOffset() == 1;
		assert streamIt.next().getId() == 11;	// next
		assertCursorGetsId(streamIt.getBaseCursor(), 10);
		assert streamIt.getOffset() == 2;

		// Second batch
		assert streamIt.hasNext();
		assertCursorGetsId(streamIt.getBaseCursor(), 12);
		assert streamIt.getOffset() == 0;
		assert streamIt.next().getId() == 12;	// next
		assertCursorGetsId(streamIt.getBaseCursor(), 12);
		assert streamIt.getOffset() == 1;

		assert streamIt.hasNext();
		assertCursorGetsId(streamIt.getBaseCursor(), 12);
		assert streamIt.getOffset() == 1;
		assert streamIt.next().getId() == 13;	// next
		assertCursorGetsId(streamIt.getBaseCursor(), 12);
		assert streamIt.getOffset() == 2;

		// Third batch
		assert streamIt.hasNext();
		assertCursorGetsId(streamIt.getBaseCursor(), 14);
		assert streamIt.getOffset() == 0;
		assert streamIt.next().getId() == 14;	// next
		assertCursorGetsId(streamIt.getBaseCursor(), 14);
		assert streamIt.getOffset() == 1;
	}

	/**
	 * Assert that fetching from the cursor gets a trivial with the specified id as the first item.
	 */
	private void assertCursorGetsId(Cursor cursor, long trivId) {
		assert ofy().load().type(Trivial.class).startAt(cursor).first().now().getId() == trivId;
	}
}
