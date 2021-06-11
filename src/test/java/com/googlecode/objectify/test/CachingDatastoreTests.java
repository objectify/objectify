/*
 */

package com.googlecode.objectify.test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.memcache.IMemcacheServiceFactory;
import com.google.appengine.spi.ServiceFactoryFactory;
import com.googlecode.objectify.cache.CachingAsyncDatastoreService;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.cache.EntityMemcache.Bucket;
import com.googlecode.objectify.test.util.MockAsyncDatastoreService;
import com.googlecode.objectify.test.util.TestBase;

/**
 * Tests of the caching datastore directly, outside of the rest of Objectify. Tries to create as few
 * future wrappers as possible so we can easily see what is going on.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingDatastoreTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CachingDatastoreTests.class.getName());
	
	/** Caching */
	CachingAsyncDatastoreService cads;
	
	/** No datastore */
	CachingAsyncDatastoreService nods;

	/** Simple bits of data to use */
	Key key;
	Set<Key> keyInSet;
	Entity entity;
	List<Entity> entityInList;

	/**
	 */
	@BeforeMethod
	public void setUpExtra()
	{
		EntityMemcache mc = new EntityMemcache(null);
		cads = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);
		nods = new CachingAsyncDatastoreService(new MockAsyncDatastoreService(), mc);
		
		key = KeyFactory.createKey("thing", 1);
		keyInSet = Collections.singleton(key);
		entity = new Entity(key);
		entity.setProperty("foo", "bar");
		entityInList = Collections.singletonList(entity);
	}
	
	/** */
	@Test
	public void testNegativeCache() throws Exception
	{
		Future<Map<Key, Entity>> fent = cads.get(null, keyInSet);
		assert fent.get().isEmpty();
		
		// Now that it's called, make sure we have a negative cache entry
		Future<Map<Key, Entity>> cached = nods.get(null, keyInSet);
		assert cached.get().isEmpty();
	}

	/** */
	@Test
	public void testBasicCache() throws Exception
	{
		Future<List<Key>> fkey = cads.put(null, entityInList);
		List<Key> putResult = fkey.get();
		
		Future<Map<Key, Entity>> fent = cads.get(null, putResult);
		assert fent.get().values().iterator().next().getProperty("foo").equals("bar");
		
		// Now make sure it is in the cache
		Future<Map<Key, Entity>> cached = nods.get(null, putResult);
		assert cached.get().values().iterator().next().getProperty("foo").equals("bar");
	}
	
	private ArgumentMatcher<Collection<Bucket>> singleBucketWithKey(Key key) {
		return new ArgumentMatcher<Collection<Bucket>>() {

			@Override
			public boolean matches(Object argument) {
				Collection<Bucket> buckets = (Collection<Bucket>) argument;
				return buckets.stream().anyMatch(b -> b.getKey().equals(key));
			}
		};
	}
	
	@Test
	public void testEmptiedOnConcurrentWrite() throws Exception
	{
		EntityMemcache mc = Mockito.spy(new EntityMemcache(null));
		CachingAsyncDatastoreService cds = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);

		Future<List<Key>> fkey = cds.put(null, entityInList);
		List<Key> putResult = fkey.get();
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// put a changed entity with the same key to memcache to simulate a memcache put by a concurrent request with an updated entity state
				ServiceFactoryFactory.getFactory(IMemcacheServiceFactory.class).getMemcacheService(null).put(KeyFactory.keyToString(key), new Entity(key));
				// in the 'actual' invocation then the 'putIfUntouched' will detect an update, so the 'putAll' call will empty the cache entry
				return (Void) invocation.callRealMethod();
			}
		}).when(mc).putAll(Mockito.argThat(singleBucketWithKey(putResult.get(0))));

		// When we get the entity, the actual state is properly returned
		Future<Map<Key, Entity>> fent = cds.get(null, putResult);
		assert fent.get().values().iterator().next().getProperty("foo").equals("bar");
		
		// and the cache is emptied because the concurrent cache write simulated with the spy is detected
		assert mc.getAll(putResult).values().iterator().next().isEmpty();
	}

	@Test
	public void testNegativeEntryConcurrentlyBecomingPositive() throws Exception
	{
		EntityMemcache mc = Mockito.spy(new EntityMemcache(null));
		CachingAsyncDatastoreService cds = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// put a changed entity with the same key to memcache to simulate a memcache put by a concurrent request with an updated entity state
				ServiceFactoryFactory.getFactory(IMemcacheServiceFactory.class).getMemcacheService(null).put(KeyFactory.keyToString(key), new Entity(key));
				// in the 'actual' invocation then the 'putIfUntouched' will detect an update, so the 'putAll' call will empty the cache entry
				return (Void) invocation.callRealMethod();
			}
		}).when(mc).putAll(Mockito.argThat(singleBucketWithKey(key)));

		Future<Map<Key, Entity>> fent = cds.get(null, keyInSet);
		assert fent.get().values().isEmpty();

		// The cache is emptied, because a colliding concurrent cache write was detected
		// (otherwise a 'NEGATIVE' entry would have been written to the cache
		assert mc.getAll(keyInSet).values().iterator().next().isEmpty();
	}

	@Test
	public void testNotEmptiedOnConcurrentIdenticalWrite() throws Exception
	{
		EntityMemcache mc = Mockito.spy(new EntityMemcache(null));
		CachingAsyncDatastoreService cds = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);

		Future<List<Key>> fkey = cds.put(null, entityInList);
		List<Key> putResult = fkey.get();
		
		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// we use this 'putAll' call to simulate a memcache update done by a concurrent request
				invocation.callRealMethod();
				// in the 'actual' invocation then the 'putIfUntouched' will detect an update, so the 'putAll' call will empty the cache entry
				return (Void) invocation.callRealMethod();
			}
		}).when(mc).putAll(Mockito.argThat(singleBucketWithKey(putResult.get(0))));

		// When we get the entity, the actual state is returned
		Future<Map<Key, Entity>> fent = cds.get(null, putResult);
		assert fent.get().values().iterator().next().getProperty("foo").equals("bar");
		
		// but the cache is not empties, because the concurrently written cache entry is identical
		assert !mc.getAll(putResult).values().iterator().next().isEmpty();
	}
	
	@Test
	public void testNotEmptiedOnConcurrentIdenticalNegativeWrite() throws Exception
	{
		EntityMemcache mc = Mockito.spy(new EntityMemcache(null));
		CachingAsyncDatastoreService cds = new CachingAsyncDatastoreService(DatastoreServiceFactory.getAsyncDatastoreService(), mc);

		Mockito.doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				// we use this 'putAll' call to simulate a memcache update done by a concurrent request
				invocation.callRealMethod();
				// in the 'actual' invocation then the 'putIfUntouched' will detect an update, so the 'putAll' call will empty the cache entry
				return (Void) invocation.callRealMethod();
			}
		}).when(mc).putAll(Mockito.argThat(singleBucketWithKey(key)));

		Future<Map<Key, Entity>> fent = cds.get(null, keyInSet);
		assert fent.get().values().isEmpty();

		// The 'NEGATIVE' value is kept in the cache, because even though there was a concurrent cache write, the values are identical
		assert mc.getAll(keyInSet).values().iterator().next().isNegative();
	}

}