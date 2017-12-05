/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.StringValue;
import com.googlecode.objectify.cache.TriggerFuture;
import com.googlecode.objectify.impl.AsyncDatastore;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.FutureHelper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Future;

/**
 * Tests of the TriggerFuture
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class TriggerFutureTests extends TestBase {

	/** This seemed to be an issue related to the listenable but apparently not */
	@Test
	void testSimpleAsyncGetWithDatastore() throws Exception {
		final AsyncDatastore ads = asyncDatastore();

		final IncompleteKey key = datastore().newKeyFactory().setKind("thing").newKey();
		final FullEntity<?> ent = FullEntity.newBuilder(key).set("foo", StringValue.newBuilder("bar").setExcludeFromIndexes(true).build()).build();

		// Without the null txn (ie, using implicit transactions) we get a "handle 0 not found" error
		final Future<List<Key>> fut = ads.put(ent);
		fut.get();
	}
	
	/** Some weird race condition on listenable future */
	@Test
	void testRaceCondition() throws Exception {
		final AsyncDatastore ads = asyncDatastore();
		
		for (int i=0; i<100; i++) {
			final int which = i;
			final FullEntity<?> ent = FullEntity.newBuilder(datastore().newKeyFactory().setKind("thing").newKey())
					.set("foo", StringValue.newBuilder("bar" + i).setExcludeFromIndexes(true).build())
					.build();

			@SuppressWarnings("unused")
			final TriggerFuture<List<Key>> fut = new TriggerFuture<List<Key>>(ads.put(ent)) {
				@Override
				protected void trigger() {
					// This magic line makes the key get updated.  Without this line,
					// we get what looks like some sort of race condition - the error
					// happens at varying iterations.
					FutureHelper.quietGet(this);

					final IncompleteKey k = ent.getKey();
					if (!(k instanceof Key))
						throw new IllegalStateException("Failed completeness at " + which);
				}
			};
		} 
	}
}