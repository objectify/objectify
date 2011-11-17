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
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> k = null;
		
		TestObjectify tOfy = this.fact.beginTransaction();
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
		
		TestObjectify txnOfy = this.fact.beginTransaction();
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
		
		HasSimpleCollection simple3 = nonTxnOfy.load().type(HasSimpleCollection.class).id(simple.id).get();
		
		// This will fail when session caching is turned on because the nonTxnOfy doesn't
		// see the change made in the transactional session, and the fetch only hits the cache.
		assert simple2.stuff.equals(simple3.stuff);
	}

	/**
	 * This should theoretically test the case where the cache is being modified even after a concurrency failure.
	 * However, it doesn't seem to trigger even without the logic fix in ListenableFuture.
	 */
	@Test
	public void testConcurrencyFailure() throws Exception
	{
		Trivial triv = new Trivial("foo", 5);
		Key<Trivial> tk = this.fact.begin().put(triv);
		
		TestObjectify tOfy1 = this.fact.beginTransaction();
		TestObjectify tOfy2 = this.fact.beginTransaction();

		Trivial triv1 = tOfy1.get(tk);
		Trivial triv2 = tOfy2.get(tk);
		
		triv1.setSomeString("bar");
		triv2.setSomeString("shouldn't work");
		
		tOfy1.put().entity(triv1).now();
		tOfy2.put().entity(triv2).now();
		
		tOfy1.getTxn().commit();
		
		try {
			tOfy2.getTxn().commit();
			assert false;	// must throw exception
		} catch (ConcurrentModificationException ex) {}

		Trivial fetched = this.fact.begin().get(tk);
		
		// This will be fetched from the cache, and must not be the "shouldn't work"
		assert fetched.getSomeString().equals("bar");
	}
	
}