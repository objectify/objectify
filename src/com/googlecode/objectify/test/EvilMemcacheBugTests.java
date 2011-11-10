/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.MemcacheSerialization;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyOpts;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;

/**
 * Tests of a bizarre bug in Google's memcache serialization of Key objects.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EvilMemcacheBugTests extends TestBase
{
	/** */
	//private static Logger log = Logger.getLogger(EvilMemcacheBugTests.class.getName());
	
	/** */
	static class SimpleParent
	{
		@Id String id;
		
		SimpleParent(){}
		SimpleParent(String id) {
			this.id = id;
		}
		
		static Key<SimpleParent> getSimpleParentKey(String id) {
			return new Key<SimpleParent>(SimpleParent.class, id);
		}
	}

	/** */
	@Cached
	static class SimpleEntity
	{
		@Id
		String id;
		@Parent Key<SimpleParent> simpleParentKey;
		String foo = "bar";
		
		static Key<SimpleEntity> getSimpleChildKey(String id) {
			return new Key<SimpleEntity>(SimpleParent.getSimpleParentKey(id), SimpleEntity.class, id);
		}
		
		SimpleEntity() {}
		SimpleEntity(String id) {
			this.id = id;
			this.simpleParentKey = SimpleParent.getSimpleParentKey(id);
		}
	}

	/** */
	@Test
	public void testMoreSophisticatedInAndOutOfTransaction() throws Exception {
		this.fact.register(SimpleParent.class);
		this.fact.register(SimpleEntity.class);
		String simpleId = "btoc";
		
		SimpleEntity simple = new SimpleEntity(simpleId);

		Objectify nonTxnOfy = this.fact.begin();
		nonTxnOfy.put(simple);
		

		Objectify txnOfy = this.fact.beginTransaction();
		SimpleEntity simple2;
		try {
			simple2 = txnOfy.get(SimpleEntity.getSimpleChildKey(simpleId));
			simple2.foo = "joe";
			txnOfy.put(simple2);
			txnOfy.getTxn().commit();
		} finally {
			if (txnOfy.getTxn().isActive())
				txnOfy.getTxn().rollback();
		}

		SimpleEntity simple3 = nonTxnOfy.get(SimpleEntity.getSimpleChildKey(simpleId));

		assert simple2.foo.equals(simple3.foo);
	}

	/** */
	@Test
	public void testRawTransactionalCaching() throws Exception {
		// Need to register it so the entity kind becomes cacheable
		this.fact.register(SimpleEntity.class);
		
		DatastoreService ods = this.fact.getDatastoreService(new ObjectifyOpts().setGlobalCache(false));
		DatastoreService ds = this.fact.getDatastoreService(new ObjectifyOpts().setGlobalCache(true));

		// This is the weirdest thing.  If you change the *name* of one of these two keys, the test passes.
		// If the keys have the same *name*, the test fails because ent3 has the "original" property.  WTF??
		com.google.appengine.api.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
		com.google.appengine.api.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");
		
		Entity ent1 = new Entity(childKey);
		ent1.setProperty("foo", "original");
		ds.put(ent1);

		// Weirdly, this will solve the problem too
		//MemcacheService cs = MemcacheServiceFactory.getMemcacheService();
		//cs.clearAll();
		
		Transaction txn = ds.beginTransaction();
		Entity ent2;
		try {
			ent2 = ods.get(txn, childKey);
			//ent2 = new Entity(childKey);	// solves the problem
			ent2.setProperty("foo", "changed");
			ds.put(txn, ent2);
			txn.commit();
		} finally {
			if (txn.isActive())
				txn.rollback();
		}

		Entity ent3 = ds.get(childKey);
		
		assert "changed".equals(ent3.getProperty("foo"));
	}

	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Test
	public void testRawCaching() throws Exception {
		// I can not for the life of me figure out why this test passes when the
		// previous test fails.

		MemcacheService cs1 = MemcacheServiceFactory.getMemcacheService("blah");
		
		com.google.appengine.api.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
		com.google.appengine.api.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");
		
		Entity ent = new Entity(childKey);
		ent.setProperty("foo", "original");
		cs1.put(childKey, ent);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		ds.put(ent);

		Transaction txn = ds.beginTransaction();
		Entity ent2 = ds.get(txn, childKey);
		
		//Entity ent2 = (Entity)cs1.get(childKey);
		assert ent2.getProperty("foo").equals("original");
		ent2.setProperty("foo", "changed");
		
		Map<Object, Object> holder = new HashMap<Object, Object>();
		holder.put(childKey, ent2);
		cs1.putAll(holder);
		
		Map<Object, Object> fetched = cs1.getAll((Collection)Collections.singleton(childKey));
		Entity ent3 = (Entity)fetched.get(childKey);
		assert ent3.getProperty("foo").equals("changed");
	}

	/** */
	@Test
	public void testEntityKeys() throws Exception
	{
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();

		com.google.appengine.api.datastore.Key parentKeyA = KeyFactory.createKey("SimpleParent", "same");
		com.google.appengine.api.datastore.Key childKeyA = KeyFactory.createKey(parentKeyA, "SimpleEntity", "different");

		Entity entA1 = new Entity(childKeyA);
		ds.put(entA1);

		Entity entA2 = ds.get(childKeyA);

		assert new String(MemcacheSerialization.makePbKey(entA1.getKey())).equals(new String(MemcacheSerialization.makePbKey(childKeyA)));
		assert new String(MemcacheSerialization.makePbKey(entA2.getKey())).equals(new String(MemcacheSerialization.makePbKey(childKeyA)));

		com.google.appengine.api.datastore.Key parentKeyB = KeyFactory.createKey("SimpleParent", "same");
		com.google.appengine.api.datastore.Key childKeyB = KeyFactory.createKey(parentKeyB, "SimpleEntity", "same");

		Entity entB1 = new Entity(childKeyB);
		ds.put(entB1);

		Entity entB2 = ds.get(childKeyB);

		// This works
		assert new String(MemcacheSerialization.makePbKey(entB1.getKey())).equals(new String(MemcacheSerialization.makePbKey(childKeyB)));
		
		// This fails!  It is a bug in the datastore.  See http://code.google.com/p/googleappengine/issues/detail?id=2088
		// Objectify works around this problem, so it is not a serious issue.
		// Update: This succeeds!  As of SDK 1.6.0 this has been fixed.  The Objectify workaround (stringifying keys) has been removed.
		assert new String(MemcacheSerialization.makePbKey(entB2.getKey())).equals(new String(MemcacheSerialization.makePbKey(childKeyB)));
	}
	
//	/** The comment was wrong - the grabTail method was removed in GAE SDK 1.6.0! */
//	@SuppressWarnings("rawtypes")
//	@Test
//	public void testWithoutObjectify()  throws Exception {
//		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
//		MemcacheService ms = MemcacheServiceFactory.getMemcacheService("testing1423");
//
//		com.google.appengine.api.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
//		com.google.appengine.api.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");
//
//		//save a test entity
//		Entity ent1 = new Entity(childKey);
//		ent1.setProperty("foo", "original");
//		ds.put(null, ent1);
//		ms.put(ent1.getKey(), ent1);
//
//		//load it, and change the value.
//		Entity ent1loaded = ds.get(null, ent1.getKey());
//		ent1loaded.setProperty("foo", "changed");
//		//save it to the datastore and memcache
//		ds.put(null, ent1loaded);
//		ms.put(ent1loaded.getKey(), ent1loaded);
//
//		//dump memcache -- silly GAE can't hide this method from me!
//		Method meth = ms.getClass().getMethod("grabTail", int.class);
//		meth.setAccessible(true);
//		List dump = (List)meth.invoke(ms, 100);
//		for(Object obj : dump)
//			log.info(obj.toString());
//		
//		assert (dump.size() == 1);
////		Entity ent3 = (Entity) ms.getAll((Collection)Collections.singleton(childKey)).values().toArray()[0];
//		
////		assert "changed".equals(ent3.getProperty("foo"));
//		
//	}
}