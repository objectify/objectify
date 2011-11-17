package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import org.testng.annotations.Test;
import org.testng.v6.Maps;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Test persisting objects that contain embedded maps.
 */
public class EmbeddedMapTests extends TestBase
{
	@com.googlecode.objectify.annotation.Entity
	public static class PojoWithMap
	{
		@Id
		Long id;
		@Embed
		Thing oneThing;
		@Embed
		Map<String, Thing> things = Maps.newHashMap();
	}

	public static class Thing
	{
		String name;
		Long weight;
	}

	@com.googlecode.objectify.annotation.Entity
	public static class PojoWithEmbeededPojoWithMap
	{
		@Id
		Long id;
		@Embed
		PojoWithMap mapPojo;
	}

	@com.googlecode.objectify.annotation.Entity
	public static class PojoWithMapWithPojoWithMap
	{
		@Id
		Long id;
		@Embed
		Map<String, PojoWithMap> nested = Maps.newHashMap();
	}

	@com.googlecode.objectify.annotation.Entity
	public static class PojoWithPrimitiveValueMap
	{
		@Id
		Long id;
		@Embed
		@AlsoLoad("simpletons")
		Map<String, Object> primitives = Maps.newHashMap();
	}

	@Test
	public void testEmbeddedMap() throws Exception
	{
		this.fact.register(PojoWithMap.class);

		TestObjectify ofy = this.fact.begin();

		PojoWithMap hasMap = createPojoWithMap("");

		Key<PojoWithMap> key = ofy.put().entity(hasMap).now();

		PojoWithMap retrieved = ofy.load().entity(key).get();
		printRawEntity(ofy, key);

		checkPojoWithMap(retrieved, "");
	}

	/**
	 * @param retrieved
	 */
	private void checkPojoWithMap(PojoWithMap retrieved, String suffix)
	{
		assert retrieved.oneThing.name.equals("Chair" + suffix);
		assert retrieved.oneThing.weight == 15L + suffix.length();
		assert retrieved.things.size() == 2;
		Thing fishThing = retrieved.things.get("fish" + suffix);
		assert fishThing.name.equals("Blue whale" + suffix);
		assert fishThing.weight.equals(1000000L + suffix.length());
		Thing fruitThing = retrieved.things.get("fruit" + suffix);
		assert fruitThing.name.equals("Apple" + suffix);
		assert fruitThing.weight.equals(1L + suffix.length());
	}

	private PojoWithMap createPojoWithMap(String suffix)
	{
		PojoWithMap hasMap = new PojoWithMap();
		hasMap.oneThing = new Thing();
		hasMap.oneThing.name = "Chair" + suffix;
		hasMap.oneThing.weight = 15L + suffix.length();
		Thing fishThing = new Thing();
		fishThing.name = "Blue whale" + suffix;
		fishThing.weight = 1000000L + suffix.length();
		Thing fruitThing = new Thing();
		fruitThing.name = "Apple" + suffix;
		fruitThing.weight = 1L + suffix.length();
		hasMap.things.put("fish" + suffix, fishThing);
		hasMap.things.put("fruit" + suffix, fruitThing);
		return hasMap;
	}

	@Test
	public void testEmbeddedPojoMap() throws Exception
	{
		this.fact.register(PojoWithEmbeededPojoWithMap.class);

		PojoWithEmbeededPojoWithMap pojo = new PojoWithEmbeededPojoWithMap();
		pojo.mapPojo = createPojoWithMap("");

		TestObjectify ofy = this.fact.begin();
		Key<PojoWithEmbeededPojoWithMap> key = ofy.put().entity(pojo).now();
		printRawEntity(ofy, key);

		pojo = ofy.load().entity(key).get();

		checkPojoWithMap(pojo.mapPojo, "");
	}

	private void printRawEntity(TestObjectify ofy, Key<?> key) throws EntityNotFoundException
	{
		Entity ent = ds().get(fact.getRawKey(key));
		System.out.println(ent);
	}

	@Test
	public void testNestedMap() throws Exception
	{
		this.fact.register(PojoWithMapWithPojoWithMap.class);
		PojoWithMap withMap = createPojoWithMap("1");
		PojoWithMapWithPojoWithMap nestedMap = new PojoWithMapWithPojoWithMap();
		nestedMap.nested.put("key", withMap);
		nestedMap.nested.put("key_2", createPojoWithMap("_2"));

		TestObjectify ofy = this.fact.begin();
		Key<PojoWithMapWithPojoWithMap> key = ofy.put().entity(nestedMap).now();
		printRawEntity(ofy, key);

		nestedMap = ofy.load().entity(key).get();

		assert nestedMap.nested.size() == 2;
		checkPojoWithMap(nestedMap.nested.get("key"), "1");
		checkPojoWithMap(nestedMap.nested.get("key_2"), "_2");
	}

	@Test
	public void testPrimitivesMap() throws Exception
	{
		this.fact.register(PojoWithPrimitiveValueMap.class);

		PojoWithPrimitiveValueMap primitiveMap = new PojoWithPrimitiveValueMap();
		primitiveMap.primitives.put("string", "Hello World");
		primitiveMap.primitives.put("number", 12);
		Date mydate = new Date();
		primitiveMap.primitives.put("date", mydate);
		ArrayList<Long> list = new ArrayList<Long>();
		list.add(1L);
		list.add(2L);
		list.add(3L);
		primitiveMap.primitives.put("list", list);

		TestObjectify ofy = this.fact.begin();
		Key<PojoWithPrimitiveValueMap> key = ofy.put().entity(primitiveMap).now();
		printRawEntity(ofy, key);

		primitiveMap = ofy.load().entity(key).get();

		assert primitiveMap.primitives.get("string").equals("Hello World");
		// Numbers in maps loose their type and get to be Longs :-(
		assert primitiveMap.primitives.get("number").equals(12L);
		assert primitiveMap.primitives.get("date").equals(mydate);
		assert primitiveMap.primitives.get("list").equals(list);
	}

	@Test
	public void testAlsoLoadWithMaps()
	{
		this.fact.register(PojoWithPrimitiveValueMap.class);

		TestObjectify ofy = this.fact.begin();
		DatastoreService ds = ds();

		Entity ent = new Entity(Key.getKind(PojoWithPrimitiveValueMap.class));
		ent.setProperty("simpletons.string", "Hello World");
		Date date = new Date();
		ent.setProperty("primitives.date", date);
		ds.put(ent);

		Key<PojoWithPrimitiveValueMap> key = Key.create(ent.getKey());
		PojoWithPrimitiveValueMap fetched = ofy.load().entity(key).get();

		assert fetched.primitives.get("string").equals("Hello World");
		assert fetched.primitives.get("date").equals(date);
	}

	@Test
	public void testDotsForbidden()
	{
		this.fact.register(PojoWithMap.class);
		PojoWithMap pojo = new PojoWithMap();

		pojo.things.put("illegal.value", new Thing());

		TestObjectify ofy = this.fact.begin();
		try
		{
			ofy.put().entity(pojo).now();
			assert false;
		}
		catch (IllegalStateException e)
		{
			// expected
			assert e.getMessage().contains("Cannot store keys with '.'");
			assert e.getMessage().contains("PojoWithMap.things");
		}
	}

	@Test
	public void testNullKeysForbidden()
	{
		this.fact.register(PojoWithMap.class);
		PojoWithMap pojo = new PojoWithMap();

		pojo.things.put(null, new Thing());

		TestObjectify ofy = this.fact.begin();
		try
		{
			ofy.put().entity(pojo).now();
			assert false;
		}
		catch (IllegalStateException e)
		{
			// expected
			assert e.getMessage().contains("Cannot store null keys");
			assert e.getMessage().contains("PojoWithMap.things");
		}
	}

	@Test
	public void testNullValuesIgnored() throws Exception
	{
		this.fact.register(PojoWithMap.class);
		PojoWithMap pojo = new PojoWithMap();

		pojo.things.put("test", null);

		TestObjectify ofy = this.fact.begin();
		Key<PojoWithMap> key = ofy.put().entity(pojo).now();
		Entity ent = ds().get(fact.getRawKey(key));

		assert !ent.hasProperty("things.test");

		pojo = ofy.load().entity(key).get();
		assert !pojo.things.containsKey("test");
	}
}
