/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.cache.TriggerFuture;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.util.FutureHelper;
import org.junit.jupiter.api.Test;

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
		final AsyncDatastoreService ads = DatastoreServiceFactory.getAsyncDatastoreService();

		final Entity ent = new Entity("thing");
		ent.setUnindexedProperty("foo", "bar");
		
		// Without the null txn (ie, using implicit transactions) we get a "handle 0 not found" error
		final Future<Key> fut = ads.put(null, ent);
		fut.get();
	}
	
	/** Some weird race condition on listenable future */
	@Test
	void testRaceCondition() throws Exception {
		final AsyncDatastoreService ads = DatastoreServiceFactory.getAsyncDatastoreService();
		
		for (int i=0; i<100; i++) {
			final int which = i;
			final Entity ent = new Entity("thing");
			ent.setUnindexedProperty("foo", "bar" + i);
			
			@SuppressWarnings("unused")
			final TriggerFuture<Key> fut = new TriggerFuture<Key>(ads.put(null, ent)) {
				@Override
				protected void trigger() {
					// This magic line makes the key get updated.  Without this line,
					// we get what looks like some sort of race condition - the error
					// happens at varying iterations.
					FutureHelper.quietGet(this);

					final Key k = ent.getKey();
					if (!k.isComplete())
						throw new IllegalStateException("Failed completeness at " + which);
				}
			};
		} 
	}
}