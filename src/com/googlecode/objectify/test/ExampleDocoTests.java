package com.googlecode.objectify.test;

import java.util.Arrays;
import java.util.List;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.entity.Person;
import com.googlecode.objectify.test.entity.Town;

/**
 * Tests that a certain-shaped Town results in the correct datastore Entity.
 * The to-string of the Entity is also cut-and-pasted into the Wiki documentation
 */
public class ExampleDocoTests extends TestBase
{
	@SuppressWarnings("unchecked")
	@Test
	public void testFirstExample() throws Exception
	{
		com.google.appengine.api.datastore.Entity e;
		Town town;

		System.out.println("First example:");

		town = new Town();
		town.name = "Springfield";
		town.mayor = new Person(new Name("Joe", "Quimby"), 53);
		town.folk = new Person[]{
				new Person(new Name("Homer", "Simpson"), 39),
				new Person(new Name("Apu", "Nahasapeemapetilon"), 48)
		};

		e = townToEntity(town);
		System.out.println(e);
		assert e.getProperties().size() == 7;
		assert e.getProperty("name").equals("Springfield");
		assert e.getProperty("mayor.name.firstName").equals("Joe");
		assert e.getProperty("mayor.name.lastName").equals("Quimby");
		assert ((Number)e.getProperty("mayor.age")).intValue() == 53;	// might be Integer or Long
		
		List<Number> ages = (List<Number>)e.getProperty("folk.age");
		assert ages.size() == 2;
		assert ages.get(0).intValue() == 39;
		assert ages.get(1).intValue() == 48;
		
		assert arrayPropertyEqual(e, "folk.name.firstName", "Homer", "Apu");
		assert arrayPropertyEqual(e, "folk.name.lastName", "Simpson", "Nahasapeemapetilon");

		town = loadTown(e);

		assert town != null;
		assert town.name != null;
		assert town.name.equals("Springfield");

		assert town.mayor != null;
		assert town.mayor.name != null;
		assert town.mayor.name.firstName.equals("Joe");
		assert town.mayor.name.lastName.equals("Quimby");
		assert town.mayor.age == 53;

		assert town.folk != null;
		assert town.folk.length == 2;
		assert town.folk[0].name != null;
		assert town.folk[0].name.firstName.equals("Homer");
		assert town.folk[0].name.lastName.equals("Simpson");
		assert town.folk[0].age == 39;
		assert town.folk[1].name != null;
		assert town.folk[1].name.firstName.equals("Apu");
		assert town.folk[1].name.lastName.equals("Nahasapeemapetilon");
		assert town.folk[1].age == 48;

	}

	@Test
	public void testNullEmbedded2() throws Exception
	{
		com.google.appengine.api.datastore.Entity e;
		Town town;

		System.out.println("Null embedded 2:");

		town = new Town();
		town.name = null;
		town.mayor = new Person(new Name("Joe", null), 53);
		town.folk = null;
		e = townToEntity(town);
		System.out.println(e);
		assert e.getProperties().size() == 4;
		assert e.hasProperty("name") && e.getProperty("name") == null;
		assert e.getProperty("mayor.name.firstName").equals("Joe");
		assert e.hasProperty("mayor.name.lastName") && e.getProperty("mayor.name.lastName") == null;
		assert ((Number)e.getProperty("mayor.age")).intValue() == 53;	// might be Integer or Long
		assert !e.hasProperty("folk");

		town = loadTown(e);

		assert town != null;
		assert town.name == null;

		assert town.mayor != null;
		assert town.mayor.name != null;
		assert town.mayor.name.firstName.equals("Joe");
		assert town.mayor.name.lastName == null;
		assert town.mayor.age == 53;

		assert town.folk == null; // null collections are left alone
	}

	@Test
	public void testNullEmbedded1() throws Exception
	{
		com.google.appengine.api.datastore.Entity e;
		Town town;

		System.out.println("Null embedded:");

		town = new Town();
		town.name = "Springfield";
		town.mayor = null;
		town.folk = new Person[0];

		e = townToEntity(town);
		System.out.println(e);
		assert e.getProperties().size() == 2;
		assert e.getProperty("name").equals("Springfield");
		assert e.hasProperty("mayor") && e.getProperty("mayor") == null;

		town = loadTown(e);

		assert town != null;
		assert town.name != null;
		assert town.name.equals("Springfield");

		assert town.mayor == null;

		assert town.folk == null; // null collections are ignored
	}

	private Town loadTown(Entity e) throws EntityNotFoundException
	{
		Key<Town> k = new Key<Town>(e.getKey());
		return fact.begin().get(k);
	}

	private boolean arrayPropertyEqual(Entity e, String prop, Object... vals)
	{
		List<?> found = (List<?>) e.getProperty(prop);
		return Arrays.asList(vals).equals(found);
	}

	private com.google.appengine.api.datastore.Entity townToEntity(Town town)
			throws EntityNotFoundException
	{
		Objectify ofy = fact.begin();
		Key<Town> k = ofy.put(town);
		return ofy.getDatastore().get(fact.getRawKey(k));
	}
}
