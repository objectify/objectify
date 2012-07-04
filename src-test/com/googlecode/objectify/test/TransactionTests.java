/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.Trivial;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;
import com.googlecode.objectify.test.util.TestObjectify.Work;

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
	private static Logger log = Logger.getLogger(TransactionTests.class.getName());
	
	/** */
	@Test
	public void testSimpleTransaction() throws Exception
	{
		this.fact.register(Trivial.class);

		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = null;
		
		TestObjectify tOfy = this.fact.begin().transaction();
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
		
		TestObjectify ofy = this.fact.begin();
		Trivial fetched = ofy.get(k);
		
		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}
	
	/** */
	@Entity
	@Cache
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
		
		TestObjectify nonTxnOfy = this.fact.begin();
		nonTxnOfy.put(simple);
		
		TestObjectify txnOfy = this.fact.begin().transaction();
		HasSimpleCollection simple2;
		try
		{
			simple2 = txnOfy.load().type(HasSimpleCollection.class).id(simple.id).get();
			simple2.stuff.add("blah");
			txnOfy.put(simple2);
			txnOfy.getTxn().commit();
		}
		finally
		{
			if (txnOfy.getTxn().isActive())
				txnOfy.getTxn().rollback();
		}
		
		nonTxnOfy.clear();
		HasSimpleCollection simple3 = nonTxnOfy.load().type(HasSimpleCollection.class).id(simple.id).get();
		
		assert simple2.stuff.equals(simple3.stuff);
	}

	/**
	 * This should theoretically test the case where the cache is being modified even after a concurrency failure.
	 * However, it doesn't seem to trigger even without the logic fix in ListenableFuture.
	 */
	@Test
	public void testConcurrencyFailure() throws Exception
	{
		this.fact.register(Trivial.class);
		
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> tk = this.fact.begin().put(triv);
		
		TestObjectify tOfy1 = this.fact.begin().transaction();
		TestObjectify tOfy2 = this.fact.begin().transaction();

		Trivial triv1 = tOfy1.get(tk);
		Trivial triv2 = tOfy2.get(tk);
		
		triv1.setSomeString("bar");
		triv2.setSomeString("shouldn't work");
		
		tOfy1.save().entity(triv1).now();
		tOfy2.save().entity(triv2).now();
		
		tOfy1.getTxn().commit();
		
		try {
			tOfy2.getTxn().commit();
			assert false;	// must throw exception
		} catch (ConcurrentModificationException ex) {}

		Trivial fetched = this.fact.begin().get(tk);
		
		// This will be fetched from the cache, and must not be the "shouldn't work"
		assert fetched.getSomeString().equals("bar");
	}

	/**
	 */
	@Test
	public void testTransactWork() throws Exception
	{
		this.fact.register(Trivial.class);
		TestObjectify ofy = this.fact.begin();
		
		final Trivial triv = new Trivial("foo", 5);
		ofy.put(triv);
		
		Trivial updated = ofy.transact(new Work<Trivial>() {
			@Override
			public Trivial run(TestObjectify ofy) {
				Trivial result = ofy.load().entity(triv).get();
				result.setSomeNumber(6);
				ofy.put(result);
				return result;
			}
		});
		assert updated.getSomeNumber() == 6;
		
		Trivial fetched = ofy.load().entity(triv).get();
		assert fetched.getSomeNumber() == 6;
	}

	/**
	 * Make sure that an async delete in a transaction fixes the session cache when the transaction is committed.
	 */
	@Test
	public void testAsyncDelete() throws Exception {
		this.fact.register(Trivial.class);
		
		final Trivial triv = new Trivial("foo", 5);
		
		// Make sure it's in the session (and memcache for that matter)
		this.putClearGet(triv);
		
		TestObjectify ofy = this.fact.begin();
		
		ofy.transact(new Work<Void>() {
			@Override
			public Void run(TestObjectify ofy) {
				// Load this, enlist in txn
				Trivial fetched = ofy.load().entity(triv).get();
				
				// Do this async, don't complete it manually
				ofy.delete().entity(fetched);
				
				return null;
			}
		});
		
		assert ofy.load().entity(triv).get() == null;
	}
	
	/** For transactionless tests */
	@Entity
	@Cache
	public static class Thing {
		@Id long id;
		String foo;
		public Thing() {}
		public Thing(long id) { this.id = id; this.foo = "foo"; }
	}
	
	/** */
	@Test
	public void testTransactionless() throws Exception
	{
		this.fact.register(Thing.class);
		TestObjectify ofy = this.fact.begin();

		for (int i=1; i<10; i++) {
			Thing th = new Thing(i);
			ofy.put(th);
		}

		ofy.transact(new Work<Void>() {
			@Override
			public Void run(TestObjectify ofy) {
				for (int i=1; i<10; i++)
					ofy.transactionless().load().type(Thing.class).id(i).get();
				
				ofy.put(new Thing(99));
				return null;
			}
		});
	}
	
}