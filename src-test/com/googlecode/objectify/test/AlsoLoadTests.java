/*
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.test.entity.HasAlsoLoads;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

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
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class HasEmbedded
	{
		@Id Long id;
		@AlsoLoad("oldFieldUser") @Embed HasAlsoLoadField fieldUser;
		@AlsoLoad("oldMethodUser") @Embed HasAlsoLoadMethod methodUser;
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class HasEmbeddedArray
	{
		@Id Long id;
		@AlsoLoad("oldFieldUsers") @Embed HasAlsoLoadField[] fieldUsers;
		@AlsoLoad("oldMethodUsers") @Embed HasAlsoLoadMethod[] methodUsers;
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
		TestObjectify ofy = this.fact.begin();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);
		
		Key<HasAlsoLoads> key = Key.create(ent.getKey());
		HasAlsoLoads fetched = ofy.load().entity(key).get();
		
		assert fetched.getStuff().equals("oldStuff");
		assert fetched.getOtherStuff() == null;
	}
	
	/** */
	@Test
	public void testAlsoLoadDuplicateError() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);
		
		try
		{
			Key<HasAlsoLoads> key = Key.create(ent.getKey());
			ofy.load().entity(key).get();
			assert false: "Shouldn't be able to read data duplicated with @AlsoLoad";
		}
		catch (Exception ex) {}
	}

	/** */
	@Test
	public void testAlsoLoadMethods() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("weirdStuff", "5");
		ds().put(ent);
		
		Key<HasAlsoLoads> key = Key.create(ent.getKey());
		HasAlsoLoads fetched = ofy.load().entity(key).get();
		
		assert fetched.getWeird() == 5;
	}
	
	/** */
	@Test
	public void testEasyHasEmbedded() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("fieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("methodUser.oldFoo", TEST_VALUE);
		ds().put(ent);
		
		Key<HasEmbedded> key = Key.create(ent.getKey());
		HasEmbedded fetched = ofy.load().entity(key).get();
		
		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testHarderHasEmbedded() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("oldFieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("oldMethodUser.oldFoo", TEST_VALUE);
		ds().put(ent);
		
		Key<HasEmbedded> key = Key.create(ent.getKey());
		HasEmbedded fetched = ofy.load().entity(key).get();
		
		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testEasyHasEmbeddedArray() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		ent.setProperty("fieldUsers.oldFoo", values);
		ent.setProperty("methodUsers.oldFoo", values);
		ds().put(ent);
		
		Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		HasEmbeddedArray fetched = ofy.load().entity(key).get();
		
		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}

	/** */
	@Test
	public void testHarderHasEmbeddedArray() throws Exception
	{
		TestObjectify ofy = this.fact.begin();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		ent.setProperty("oldFieldUsers.oldFoo", values);
		ent.setProperty("oldMethodUsers.oldFoo", values);
		ds().put(ent);
		
		Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		HasEmbeddedArray fetched = ofy.load().entity(key).get();
		
		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}
}