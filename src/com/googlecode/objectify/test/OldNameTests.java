/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.test.entity.TrivialWithId;

/**
 * Tests of using the @OldName annotation
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OldNameTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(OldNameTests.class);
	
	/** */
	@Test
	public void testSimpleOldName() throws Exception
	{
		Objectify ofy = ObjectifyFactory.get();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(ObjectifyFactory.getKind(TrivialWithId.class));
		ent.setProperty("someNumber", 1);
		ent.setProperty("someString", 2);	// setting a number
		ds.put(ent);
		
		TrivialWithId fetched = ofy.get(ent.getKey());
		
		assert fetched.getSomeNumber() == 1;
		assert fetched.getSomeString().equals("2");	// should be a string
	}
}