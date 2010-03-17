/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Unsaved;
import com.googlecode.objectify.condition.IfTrue;

/**
 * Tests of using the @Unsaved annotation and its various conditions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class UnsavedTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(UnsavedTests.class);
	
	/** */
	public static final String TEST_VALUE = "blah";
	
	/** Just making sure it works when we have deeper inheritance */
	static class DeeperIfTrue extends IfTrue {}
	
	/** */
	@Cached
	static class CompletelyUnsaved
	{
		@Id Long id;
		@Unsaved String foo;
	}
	
	/** */
	@Test
	public void testCompletelyUnsaved() throws Exception
	{
		this.fact.register(CompletelyUnsaved.class);
		
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(CompletelyUnsaved.class));
		ent.setProperty("foo", TEST_VALUE);
		ds.put(ent);
		
		Key<CompletelyUnsaved> key = this.fact.rawKeyToTypedKey(ent.getKey());
		CompletelyUnsaved fetched = ofy.get(key);
		assert fetched.foo.equals(TEST_VALUE);
		
		fetched = putAndGet(fetched);
		assert fetched.foo == null;
	}
	
	/** */
	@Cached
	static class UnsavedWhenTrue
	{
		@Id Long id;
		@Unsaved(IfTrue.class) boolean foo;
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
		assert fetched.foo == false;
		assert fetched.bar == true;
	}

	/** */
	@Cached
	static class DeeperUnsavedWhenTrue
	{
		@Id Long id;
		@Unsaved(IfTrue.class) boolean foo;
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
		assert fetched.foo == false;
		assert fetched.bar == true;
	}

	/** Should not be registerable */
	static class BadFieldType
	{
		@Id Long id;
		@Unsaved(IfTrue.class) String foo;
	}
	
	/** Should not be registerable */
	static class DeeperBadFieldType
	{
		@Id Long id;
		@Unsaved(DeeperIfTrue.class) String foo;
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
	}
}