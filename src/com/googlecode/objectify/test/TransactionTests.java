/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Id;

import org.testng.annotations.Test;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
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
	private static Logger log = Logger.getLogger(TransactionTests.class.getName());
	
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
}