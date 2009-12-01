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
import com.googlecode.objectify.test.entity.TrivialWithName;

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
	public void testGenerateId() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		TrivialWithId triv = new TrivialWithId(5, "foo");
		Key k = ofy.put(triv);
		
		assert k.getKind().equals(ObjectifyFactory.getKind(triv.getClass()));
		assert k.getId() == triv.getId();
		
		Key created = ObjectifyFactory.createKey(TrivialWithId.class, k.getId());
		assert k.equals(created);
		
		TrivialWithId fetched = ofy.get(k);
		
		assert fetched.getId().equals(k.getId());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}

	/** */
	@Test
	public void testOverwriteId() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		TrivialWithId triv = new TrivialWithId(5, "foo");
		Key k = ofy.put(triv);
		
		TrivialWithId triv2 = new TrivialWithId(k.getId(), 6, "bar");
		Key k2 = ofy.put(triv2);
		
		assert k2.equals(k);
		
		TrivialWithId fetched = ofy.get(k);
		
		assert fetched.getId() == k.getId();
		assert fetched.getSomeNumber() == triv2.getSomeNumber();
		assert fetched.getSomeString().equals(triv2.getSomeString());
	}

	/** */
	@Test
	public void testNames() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		
		TrivialWithName triv = new TrivialWithName("first", 5, "foo");
		Key k = ofy.put(triv);

		assert k.getName().equals("first");
		
		Key createdKey = ObjectifyFactory.createKey(TrivialWithName.class, "first");
		assert k.equals(createdKey);
		
		TrivialWithName fetched = ofy.get(k);
		
		assert fetched.getName().equals(k.getName());
		assert fetched.getSomeNumber() == triv.getSomeNumber();
		assert fetched.getSomeString().equals(triv.getSomeString());
	}
}