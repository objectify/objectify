/*
 */

package com.googlecode.objectify.test;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;

/**
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(CachingTests.class.getName());

	/** */
	@Entity
	static class Uncached {
		@Id Long id;
		String stuff;
	}

	/** */
	@Entity
	@Cache
	static class Cached {
		@Id Long id;
		String stuff;
	}

	/**
	 */
	@BeforeMethod
	public void setUp() {
		super.setUp();

		fact().register(Uncached.class);
		fact().register(Cached.class);
	}

	/** */
	@Test
	public void testHeterogeneousBatch() throws Exception {
		Uncached un1 = new Uncached();
		un1.stuff = "un1 stuff";

		Uncached un2 = new Uncached();
		un2.stuff = "un2 stuff";

		Cached ca1 = new Cached();
		ca1.stuff = "ca1 stuff";

		Cached ca2 = new Cached();
		ca2.stuff = "ca2 stuff";

		List<Object> entities = new ArrayList<Object>();
		entities.add(un1);
		entities.add(ca1);
		entities.add(un2);
		entities.add(ca2);

		Map<Key<Object>, Object> keys = ofy().save().entities(entities).now();
		ofy().clear();
		Map<Key<Object>, Object> fetched = ofy().load().keys(keys.keySet());

		assert fetched.size() == 4;
		assert fetched.containsKey(Key.create(un1));
		assert fetched.containsKey(Key.create(un2));
		assert fetched.containsKey(Key.create(ca1));
		assert fetched.containsKey(Key.create(ca2));
	}
	
	/** */
	@Entity
	@Cache(expirationSeconds=1)
	static class Expires {
		@Id Long id;
		String stuff;
	}

	/** */
	@Test
	public void cacheExpirationWorks() throws Exception {
		fact().register(Expires.class);
		
		Expires exp = new Expires();
		exp.stuff = "foo";
		
		Key<Expires> key = ofy().save().entity(exp).now();
		ofy().clear();
		ofy().load().key(key).now();	// cached now
		
		MemcacheService ms = MemcacheServiceFactory.getMemcacheService(ObjectifyFactory.MEMCACHE_NAMESPACE);
		
		Object thing = ms.get(key.getString());
		assert thing != null;
		
		Thread.sleep(2000);
		
		Object thing2 = ms.get(key.getString());
		assert thing2 == null;
	}
}