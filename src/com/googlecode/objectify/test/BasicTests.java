/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of basic entity manipulation.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BasicTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(BasicTests.class);
	
	/** */
	@Test
	public void testGenerateKey() throws Exception
	{
		Objectify ofy = ObjectifyFactory.get();
		
		Trivial triv = new Trivial(null, 5, "foo");
		Key k = ofy.put(triv);
		
		assert k.getKind().equals(ObjectifyFactory.getKind(triv.getClass()));
		assert k == triv.getKey();
		
		Trivial fetched = ofy.get(k);
		
		assert fetched.getKey().equals(k);
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testOverwriteKey() throws Exception
	{
		Objectify ofy = ObjectifyFactory.get();
		
		Trivial triv = new Trivial(null, 5, "foo");
		Key k = ofy.put(triv);
		
		Trivial triv2 = new Trivial(k, 6, "bar");
		Key k2 = ofy.put(triv2);
		
		assert k2 == k;
		
		Trivial fetched = ofy.get(k);
		
		assert fetched.getKey().equals(k);
		assert fetched.getSomeNumber() == triv2.getSomeNumber();
		assert fetched.getSomeString().equals(triv2.getSomeString());
	}
}