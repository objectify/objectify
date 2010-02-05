/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;

/**
 * This is a set of tests that clarify exactly what happens when you put different
 * kinds of entities into the datastore.  They aren't really tests of Objectify,
 * they just help us understand the underlying behavior.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DatastoreEntityTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(DatastoreEntityTests.class);

	public static class Thing
	{
		public String name;
		public int age;
	}
	
	@SuppressWarnings("serial")
	public static class SerializableThing extends Thing implements Serializable
	{
	}
	
	/**
	 * What happens when you put an object in an Entity?
	 */
	@Test
	public void testObjectProperty() throws Exception
	{
		Thing thing = new Thing();
		thing.name = "foo";
		thing.age = 10;

		Entity ent = new Entity("Test");
		try
		{
			ent.setProperty("thing", thing);
			assert false;
		}
		catch (IllegalArgumentException ex) {}

//		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
//		ds.put(ent);
//		
//		Entity fetched = ds.get(ent.getKey());
//		Thing fetchedThing = (Thing)fetched.getProperty("thing");
//		assert thing.name.equals(fetchedThing.name);
//		assert thing.age == fetchedThing.age;
	}

	/**
	 * What happens if it is serializable?
	 */
	@Test
	public void testSerializableObjectProperty() throws Exception
	{
		SerializableThing thing = new SerializableThing();
		thing.name = "foo";
		thing.age = 10;
		
		Entity ent = new Entity("Test");
		try
		{
			ent.setProperty("thing", thing);
			assert false;
		}
		catch (IllegalArgumentException ex) {}
		
//		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
//		ds.put(ent);
//		
//		Entity fetched = ds.get(ent.getKey());
//		SerializableThing fetchedThing = (SerializableThing)fetched.getProperty("thing");
//		assert thing.name.equals(fetchedThing.name);
//		assert thing.age == fetchedThing.age;
	}
}