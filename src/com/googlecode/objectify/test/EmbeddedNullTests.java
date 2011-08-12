package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.test.entity.Criminal;
import com.googlecode.objectify.test.entity.Name;

/**
 * Tests specifically dealing with nulls in embedded fields and collections
 */
public class EmbeddedNullTests extends TestBase
{
	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		Criminal avoid = new Criminal();
		avoid.aliases = new Name[] { new Name("Bob", "Dobbs") };
		avoid.moreAliases = Collections.singletonList(new Name("Bob", "Dobbs"));
		this.fact.begin().put(avoid);
	}

	/**
	 * Rule: nulls come back as nulls
	 * Rule: filtering collections filters by contents, so looking for null fails
	 */
	@Test
	public void testNullCollection() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = null;
		crim.moreAliases = null;
		
		Criminal fetched = this.putAndGet(crim);
		assert fetched.aliases == null;
		assert fetched.moreAliases == null;
		
		// Now check the queries
		Objectify ofy = this.fact.begin();
		Iterator<Criminal> queried;

		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
		assert !queried.hasNext();

		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
		assert !queried.hasNext();

		// Potential altenate syntax?
//		queried = ofy.query(Criminal.class).filterNullCollection("aliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("moreAliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
		
//		queried = ofy.query(Criminal.class).filterEmptyCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("moreAliases").iterator();
//		assert !queried.hasNext();
	}

	/**
	 */
	@Test
	public void testEmptyCollection() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[0];
		crim.moreAliases = new ArrayList<Name>();
		
		Criminal fetched = this.putAndGet(crim);
		assert fetched.aliases == null;	// not valid with caching objectify
		assert fetched.moreAliases == null;

		// Now check the queries
		Objectify ofy = this.fact.begin();
		Iterator<Criminal> queried;

		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
		assert !queried.hasNext();

		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
		assert !queried.hasNext();
		
		// Potential altenate syntax?
//		queried = ofy.query(Criminal.class).filterNullCollection("aliases").iterator();
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterNullCollection("moreAliases").iterator();
//		assert !queried.hasNext();
		
//		queried = ofy.query(Criminal.class).filterEmptyCollection("aliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();

//		queried = ofy.query(Criminal.class).filterEmptyCollection("moreAliases").iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
	}

	/**
	 */
	@Test
	public void testCollectionContainingNull() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[] { null };
		crim.moreAliases = Arrays.asList(crim.aliases);
		
		Criminal fetched = this.putAndGet(crim);
		assert fetched.aliases != null;
		assert fetched.aliases.length == 1;
		assert fetched.aliases[0] == null;
		
		assert fetched.moreAliases != null;
		assert fetched.moreAliases.size() == 1;
		assert fetched.moreAliases.get(0) == null;

		// Queries on non-leaf values are not currently supported
//		Objectify ofy = this.fact.begin();
//		Iterator<Criminal> queried;
//		
//		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
//
//		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
	}
	
	/**
	 */
	@Test
	public void easierTestCollectionContainingNullAndOtherStuff() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[] { new Name("Bob", "Dobbs"), null, new Name("Ivan", "Stang") };
		
		Criminal fetched = this.putAndGet(crim);
		
		assert fetched.aliases != null;
		assert fetched.aliases.length == 3;
		assert fetched.aliases[0] != null;
		assert fetched.aliases[1] == null;
		assert fetched.aliases[2] != null;
	}
	
	/**
	 */
	@Test
	public void testCollectionContainingNullAndOtherStuff() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[] { new Name("Bob", "Dobbs"), null, new Name("Ivan", "Stang") };
		crim.moreAliases = Arrays.asList(crim.aliases);
		
		Criminal fetched = this.putAndGet(crim);
		
		assert fetched.aliases != null;
		assert fetched.aliases.length == 3;
		assert fetched.aliases[0] != null;
		assert fetched.aliases[1] == null;
		assert fetched.aliases[2] != null;
		
		assert fetched.moreAliases != null;
		assert fetched.moreAliases.size() == 3;
		assert fetched.moreAliases.get(0) != null;
		assert fetched.moreAliases.get(1) == null;
		assert fetched.moreAliases.get(2) != null;

		// Queries on non-leaf values are not currently supported
//		Objectify ofy = this.fact.begin();
//		Iterator<Criminal> queried;
//		
//		queried = ofy.query(Criminal.class).filter("aliases", null).iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
//
//		queried = ofy.query(Criminal.class).filter("moreAliases", null).iterator();
//		assert queried.hasNext();
//		assert queried.next().id.equals(fetched.id);
//		assert !queried.hasNext();
	}
	
	/**
	 * Reported error when a field is null in an embedded set, but it seems to work
	 */
	@Test
	public void testEmbeddedSetWithNullField() throws Exception
	{
		Criminal crim = new Criminal();
		crim.aliases = new Name[] { new Name("Bob", "Dobbs"), new Name("Mojo", null), new Name("Ivan", "Stang") };
		crim.aliasesSet = new HashSet<Name>(Arrays.asList(crim.aliases));
		
		Criminal fetched = this.putAndGet(crim);
		
		for (Name name: crim.aliases)
			assert fetched.aliasesSet.contains(name);
	}
	
}
