/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.test.entity.HasAlsoLoads;

/**
 * More tests of using the @AlsoLoad annotation
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AlsoLoadMoreTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(AlsoLoadMoreTests.class);
	
	/** */
	public static final String TEST_VALUE = "blah";
	
	/** */
	@Cached
	static class MethodOverridesField
	{
		@Id Long id;
		String foo;
		String bar;
		public void set(@AlsoLoad("foo") String overrides)
		{
			this.bar = overrides;
		}
	}
	
	/** Should not be registerable */
	static class ConflictingFields
	{
		@Id Long id;
		String foo;
		@AlsoLoad("foo") String bar;
	}
	
	/** Should not be registerable */
	static class ConflictingMethods
	{
		@Id Long id;
		public void set1(@AlsoLoad("foo") String foo1) {}
		public void set2(@AlsoLoad("foo") String foo2) {}
	}
	
	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(MethodOverridesField.class);
	}

	/** */
	@Test
	public void testMethodOverridingField() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(MethodOverridesField.class));
		ent.setProperty("foo", TEST_VALUE);
		ds.put(ent);
		
		Key<MethodOverridesField> key = this.fact.rawKeyToTypedKey(ent.getKey());
		MethodOverridesField fetched = ofy.get(key);
		
		assert fetched.foo == null;
		assert fetched.bar.equals(TEST_VALUE);
	}
	
	/** */
	@Test
	public void testNotRegisterable() throws Exception
	{
		try
		{
			this.fact.register(ConflictingFields.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
		
		try
		{
			this.fact.register(ConflictingMethods.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
	}
}