/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
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
	private static Logger log = Logger.getLogger(DatastoreEntityTests.class.getName());

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

	/**
	 * What happens when you put empty collections in an Entity?
	 */
	@Test
	public void testEmptyCollectionInEntity() throws Exception
	{
		Entity ent = new Entity("Test");
		List<Object> empty = new ArrayList<Object>();
		ent.setProperty("empty", empty);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		ds.put(ent);
		
		Entity fetched = ds.get(ent.getKey());
		
		System.out.println(fetched);
		
		Object whatIsIt = fetched.getProperty("empty");
		assert whatIsIt == null;
	}

	/**
	 * What happens when you put a single null in a collection in an Entity?
	 */
	@SuppressWarnings("unchecked")
	@Test
	public void testCollectionContainingNullInEntity() throws Exception
	{
		Entity ent = new Entity("Test");
		List<Object> hasNull = new ArrayList<Object>();
		hasNull.add(null);
		ent.setProperty("hasNull", hasNull);

		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		ds.put(ent);
		
		Entity fetched = ds.get(ent.getKey());
		
		System.out.println(fetched);
		
		Collection<Object> whatIsIt = (Collection<Object>)fetched.getProperty("hasNull");
		assert whatIsIt != null;
		assert whatIsIt.size() == 1;
		assert whatIsIt.iterator().next() == null;
	}
}