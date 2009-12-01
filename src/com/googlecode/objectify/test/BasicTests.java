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
import com.googlecode.objectify.test.entity.TrivialWithId;

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
		
		TrivialWithId triv = new TrivialWithId(5, "foo");
		Key k = ofy.put(triv);
		
		assert k.getKind().equals(ObjectifyFactory.getKind(triv.getClass()));
		assert k.getId() == triv.getId();
		
		TrivialWithId fetched = ofy.get(k);
		
		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testOverwriteKey() throws Exception
	{
		Objectify ofy = ObjectifyFactory.get();
		
		TrivialWithId triv = new TrivialWithId(5, "foo");
		Key k = ofy.put(triv);
		
		TrivialWithId triv2 = new TrivialWithId(k.getId(), 6, "bar");
		Key k2 = ofy.put(triv2);
		
		assert k2 == k;
		
		TrivialWithId fetched = ofy.get(k);
		
		assert fetched.getId() == k.getId();
		assert fetched.getSomeNumber() == triv2.getSomeNumber();
		assert fetched.getSomeString().equals(triv2.getSomeString());
	}
}