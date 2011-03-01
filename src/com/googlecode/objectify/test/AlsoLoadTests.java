/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

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
 * Tests of using the @AlsoLoad annotation
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AlsoLoadTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(AlsoLoadTests.class.getName());
	
	/** */
	public static final String TEST_VALUE = "blah";
	
	/** */
	static class HasAlsoLoadField
	{
		@AlsoLoad("oldFoo") String foo;
		
		public HasAlsoLoadField() {}
		public HasAlsoLoadField(String value) { this.foo = value; }
		
		public boolean equals(Object other)
		{
			return this.foo.equals(((HasAlsoLoadField)other).foo);
		}
	}
	
	/** */
	static class HasAlsoLoadMethod
	{
		String foo;
		
		public HasAlsoLoadMethod() {}
		public HasAlsoLoadMethod(String value) { this.foo = value; }

		public void set(@AlsoLoad("oldFoo") String oldFoo)
		{
			this.foo = oldFoo;
		}
		
		public boolean equals(Object other)
		{
			return this.foo.equals(((HasAlsoLoadMethod)other).foo);
		}
	}
	
	/** */
	@Cached
	static class HasEmbedded
	{
		@Id Long id;
		@AlsoLoad("oldFieldUser") @Embedded HasAlsoLoadField fieldUser;
		@AlsoLoad("oldMethodUser") @Embedded HasAlsoLoadMethod methodUser;
	}
	
	/** */
	@Cached
	static class HasEmbeddedArray
	{
		@Id Long id;
		@AlsoLoad("oldFieldUsers") @Embedded HasAlsoLoadField[] fieldUsers;
		@AlsoLoad("oldMethodUsers") @Embedded HasAlsoLoadMethod[] methodUsers;
	}
	
	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		
		this.fact.register(HasEmbedded.class);
		this.fact.register(HasEmbeddedArray.class);
	}

	/** */
	@Test
	public void testSimpleAlsoLoad() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		Key<HasAlsoLoads> key = new Key<HasAlsoLoads>(ent.getKey());
		HasAlsoLoads fetched = ofy.get(key);
		
		assert fetched.getStuff().equals("oldStuff");
		assert fetched.getOtherStuff() == null;
	}
	
	/** */
	@Test
	public void testAlsoLoadDuplicateError() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		try
		{
			Key<HasAlsoLoads> key = new Key<HasAlsoLoads>(ent.getKey());
			ofy.get(key);
			assert false: "Shouldn't be able to read data duplicated with @AlsoLoad";
		}
		catch (Exception ex) {}
	}

	/** */
	@Test
	public void testAlsoLoadMethods() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("weirdStuff", "5");
		ds.put(ent);
		
		Key<HasAlsoLoads> key = new Key<HasAlsoLoads>(ent.getKey());
		HasAlsoLoads fetched = ofy.get(key);
		
		assert fetched.getWeird() == 5;
	}
	
	/** */
	@Test
	public void testEasyHasEmbedded() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("fieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("methodUser.oldFoo", TEST_VALUE);
		ds.put(ent);
		
		Key<HasEmbedded> key = new Key<HasEmbedded>(ent.getKey());
		HasEmbedded fetched = ofy.get(key);
		
		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testHarderHasEmbedded() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("oldFieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("oldMethodUser.oldFoo", TEST_VALUE);
		ds.put(ent);
		
		Key<HasEmbedded> key = new Key<HasEmbedded>(ent.getKey());
		HasEmbedded fetched = ofy.get(key);
		
		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testEasyHasEmbeddedArray() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		ent.setProperty("fieldUsers.oldFoo", values);
		ent.setProperty("methodUsers.oldFoo", values);
		ds.put(ent);
		
		Key<HasEmbeddedArray> key = new Key<HasEmbeddedArray>(ent.getKey());
		HasEmbeddedArray fetched = ofy.get(key);
		
		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}

	/** */
	@Test
	public void testHarderHasEmbeddedArray() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		ent.setProperty("oldFieldUsers.oldFoo", values);
		ent.setProperty("oldMethodUsers.oldFoo", values);
		ds.put(ent);
		
		Key<HasEmbeddedArray> key = new Key<HasEmbeddedArray>(ent.getKey());
		HasEmbeddedArray fetched = ofy.get(key);
		
		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}
}