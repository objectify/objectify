/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.NotSaved;
import com.googlecode.objectify.condition.IfDefault;
import com.googlecode.objectify.condition.IfNull;
import com.googlecode.objectify.condition.IfTrue;

/**
 * Tests of using the @NotSaved annotation and its various conditions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NotSavedTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(NotSavedTests.class.getName());
	
	/** */
	public static final String TEST_VALUE = "blah";
	
	/** Just making sure it works when we have deeper inheritance */
	static class DeeperIfTrue extends IfTrue {}
	
	/** */
	@Cached
	static class CompletelyUnsaved
	{
		@Id Long id;
		@NotSaved String foo;
	}
	
	/** */
	@Test
	public void testCompletelyUnsaved() throws Exception
	{
		this.fact.register(CompletelyUnsaved.class);
		
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(CompletelyUnsaved.class));
		ent.setProperty("foo", TEST_VALUE);
		ds.put(ent);
		
		Key<CompletelyUnsaved> key = new Key<CompletelyUnsaved>(ent.getKey());
		CompletelyUnsaved fetched = ofy.get(key);
		assert fetched.foo.equals(TEST_VALUE);
		
		fetched = putAndGet(fetched);
		assert fetched.foo == null;	// will fail if session caching objectify is turned on
	}
	
	/** */
	@Cached
	static class UnsavedWhenTrue
	{
		@Id Long id;
		@NotSaved(IfTrue.class) boolean foo;
		boolean bar;
	}
	
	/** */
	@Test
	public void testUnsavedWhenTrue() throws Exception
	{
		this.fact.register(UnsavedWhenTrue.class);
		
		UnsavedWhenTrue thing = new UnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;
		
		UnsavedWhenTrue fetched = putAndGet(thing);
		assert fetched.foo == false;	// will fail with session caching turned on
		assert fetched.bar == true;
	}

	/** */
	@Cached
	static class DeeperUnsavedWhenTrue
	{
		@Id Long id;
		@NotSaved(IfTrue.class) boolean foo;
		boolean bar;
	}
	
	/** */
	@Test
	public void testDeeperUnsavedWhenTrue() throws Exception
	{
		this.fact.register(DeeperUnsavedWhenTrue.class);
		
		DeeperUnsavedWhenTrue thing = new DeeperUnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;
		
		DeeperUnsavedWhenTrue fetched = putAndGet(thing);
		assert fetched.foo == false;	// will fail with session caching objectify turned on
		assert fetched.bar == true;
	}

	/** Should not be registerable */
	static class BadFieldType
	{
		@Id Long id;
		@NotSaved(IfTrue.class) String foo;
	}
	
	/** Should not be registerable */
	static class DeeperBadFieldType
	{
		@Id Long id;
		@NotSaved(DeeperIfTrue.class) String foo;
	}
	
	/** Should not be registerable */
	static class TryToEmbedMe { @NotSaved(IfNull.class) String bar; }
	static class EmbeddedCollectionWithUnsaved
	{
		@Id Long id;
		@Embedded TryToEmbedMe[] stuff;
	}
	
	/** */
	@Test
	public void testNotRegisterable() throws Exception
	{
		try
		{
			this.fact.register(BadFieldType.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
		
		try
		{
			this.fact.register(DeeperBadFieldType.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
		
		try
		{
			this.fact.register(EmbeddedCollectionWithUnsaved.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
	}
	
	/** */
	@Cached
	static class UnsavedDefaults
	{
		@Id Long id;
		@NotSaved(IfDefault.class) boolean booleanDefault = true;
		@NotSaved(IfDefault.class) String stringDefault = TEST_VALUE;
		@NotSaved(IfDefault.class) int intDefault = 10;
		@NotSaved(IfDefault.class) float floatDefault = 10f;
	}
	
	/** */
	@Test
	public void testUnsavedDefaults() throws Exception
	{
		this.fact.register(UnsavedDefaults.class);
		
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		UnsavedDefaults thing = new UnsavedDefaults();
		Key<UnsavedDefaults> key = ofy.put(thing);
		
		// Now get the raw entity and verify that it doesn't have properties saved
		Entity ent = ds.get(this.fact.getRawKey(key));
		assert ent.getProperties().isEmpty();
	}

}