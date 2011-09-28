/*
 */

package com.googlecode.objectify.test;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.cache.TriggerFuture;
import com.googlecode.objectify.util.FutureHelper;

/**
 * Tests of the TriggerFuture
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TriggerFutureTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(TriggerFutureTests.class.getName());

	/** This seemed to be an issue related to the listenable but apparently not */
	@Test
	public void testSimpleAsyncGetWithDatastore() throws Exception
	{
		AsyncDatastoreService ads = DatastoreServiceFactory.getAsyncDatastoreService();
		
		Entity ent = new Entity("thing");
		ent.setUnindexedProperty("foo", "bar");
		
		// Without the null txn (ie, using implicit transactions) we get a "handle 0 not found" error
		Future<Key> fut = ads.put(null, ent);
		fut.get();
	}
	
	/** Some weird race condition on listenable future */
	@Test
	public void testRaceCondition() throws Exception
	{
		ObjectifyOpts opts = new ObjectifyOpts().setGlobalCache(false);
		AsyncDatastoreService ads = this.fact.getAsyncDatastoreService(opts);
		
		for (int i=0; i<100; i++)
		{
			final int which = i;
			final Entity ent = new Entity("thing");
			ent.setUnindexedProperty("foo", "bar" + i);
			
			@SuppressWarnings("unused")
			TriggerFuture<Key> fut = new TriggerFuture<Key>(ads.put(null, ent)) {
				@Override
				protected void trigger()
				{
					// This magic line makes the key get updated.  Without this line,
					// we get what looks like some sort of race condition - the error
					// happens at varying iterations.
					FutureHelper.quietGet(this);
					
					Key k = ent.getKey();
					if (!k.isComplete())
						throw new IllegalStateException("Failed completeness at " + which);
				}
			};
		} 
	}
}