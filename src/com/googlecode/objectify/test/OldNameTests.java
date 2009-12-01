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
import com.googlecode.objectify.test.entity.WithOldNames;

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
		Objectify ofy = ObjectifyFactory.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(ObjectifyFactory.getKind(WithOldNames.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		WithOldNames fetched = ofy.get(ent.getKey());
		
		assert fetched.getStuff().equals("oldStuff");
		assert fetched.getOtherStuff() == null;
	}
	
	/** */
	@Test
	public void testOldNameDuplicateError() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(ObjectifyFactory.getKind(WithOldNames.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		try
		{
			ofy.get(ent.getKey());
			assert false: "Shouldn't be able to read data duplicated with @OldName";
		}
		catch (Exception ex) {}
	}

	/** */
	@Test
	public void testOldNameMethods() throws Exception
	{
		Objectify ofy = ObjectifyFactory.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(ObjectifyFactory.getKind(WithOldNames.class));
		ent.setProperty("weirdStuff", "5");
		ds.put(ent);
		
		WithOldNames fetched = ofy.get(ent.getKey());
		
		assert fetched.getWeird() == 5;
	}
}