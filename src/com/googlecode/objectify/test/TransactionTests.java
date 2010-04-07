/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.CachingDatastoreService;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of transactional behavior.  Since many transactional characteristics are
 * determined by race conditions and other realtime effects, these tests are not
 * very thorough.  We will assume that Google's transactions work.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TransactionTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(TransactionTests.class);
	
	/** */
	@Test
	public void testSimpleTransaction() throws Exception
	{
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = null;
		
		Objectify tOfy = this.fact.beginTransaction();
		try
		{
			k = tOfy.put(triv);
			tOfy.getTxn().commit();
		}
		finally
		{
			if (tOfy.getTxn().isActive())
				tOfy.getTxn().rollback();
		}
		
		Objectify ofy = this.fact.begin();
		Trivial fetched = ofy.get(k);
		
		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}
	
	/** */
	@Cached
	static class HasSimpleCollection
	{
		@Id Long id;
		List<String> stuff = new ArrayList<String>();
	}

	/** */
	@Test
	public void testInAndOutOfTransaction() throws Exception
	{
		this.fact.register(HasSimpleCollection.class);
		
		HasSimpleCollection simple = new HasSimpleCollection();
		
		Objectify nonTxnOfy = this.fact.begin();
		nonTxnOfy.put(simple);
		
		Objectify txnOfy = this.fact.beginTransaction();
		HasSimpleCollection simple2;
		try
		{
			simple2 = txnOfy.get(HasSimpleCollection.class, simple.id);
			simple2.stuff.add("blah");
			txnOfy.put(simple2);
			txnOfy.getTxn().commit();
		}
		finally
		{
			if (txnOfy.getTxn().isActive())
				txnOfy.getTxn().rollback();
		}
		
		HasSimpleCollection simple3 = nonTxnOfy.get(HasSimpleCollection.class, simple.id);
		
		assert simple2.stuff.equals(simple3.stuff);
	}

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
		
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		ds = new CachingDatastoreService(this.fact, ds);

		// This is the weirdest thing.  If you change the *name* of one of these two keys, the test passes.
		// If the keys have the same *name*, the test fails because ent3 has the "original" property.  WTF??
		com.google.appengine.api.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
		com.google.appengine.api.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");
		
		Entity ent1 = new Entity(childKey);
		ent1.setProperty("foo", "original");
		ds.put(ent1);

		// Start transaction
		Transaction txn = ds.beginTransaction();
		Entity ent2;
		try {
			ent2 = ds.get(txn, childKey);
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
	@SuppressWarnings("unchecked")
	@Test
	public void testRawCaching() throws Exception {

		MemcacheService cs = MemcacheServiceFactory.getMemcacheService();
		cs.setNamespace("blah");
		
		com.google.appengine.api.datastore.Key parentKey = KeyFactory.createKey("SimpleParent", "asdf");
		com.google.appengine.api.datastore.Key childKey = KeyFactory.createKey(parentKey, "SimpleEntity", "asdf");

		Entity ent = new Entity(childKey);
		ent.setProperty("foo", "original");
		cs.put(childKey, ent);
		
		Entity ent2 = (Entity)cs.get(childKey);
		assert ent2.getProperty("foo").equals("original");
		ent2.setProperty("foo", "changed");
		
		Map<Object, Object> holder = new HashMap<Object, Object>();
		holder.put(childKey, ent2);
		cs.putAll(holder);
		
		Map<Object, Object> fetched = cs.getAll((Collection)Collections.singleton(childKey));
		Entity ent3 = (Entity)fetched.get(childKey);
		assert ent3.getProperty("foo").equals("changed");
	}
}