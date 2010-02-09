/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.test.entity.HasOldNames;

/**
 * Tests of using the @OldName annotation
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OldNameTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(OldNameTests.class);
	
	/** */
	public static final String TEST_VALUE = "blah";
	
	/** */
	static class HasOldNameField
	{
		@OldName("oldFoo") String foo;
		
		public HasOldNameField() {}
		public HasOldNameField(String value) { this.foo = value; }
		
		public boolean equals(Object other)
		{
			return this.foo.equals(((HasOldNameField)other).foo);
		}
	}
	
	/** */
	static class HasOldNameMethod
	{
		String foo;
		
		public HasOldNameMethod() {}
		public HasOldNameMethod(String value) { this.foo = value; }

		public void set(@OldName("oldFoo") String oldFoo)
		{
			this.foo = oldFoo;
		}
		
		public boolean equals(Object other)
		{
			return this.foo.equals(((HasOldNameMethod)other).foo);
		}
	}
	
	/** */
	@Cached
	static class HasEmbedded
	{
		@Id Long id;
		@OldName("oldFieldUser") @Embedded HasOldNameField fieldUser;
		@OldName("oldMethodUser") @Embedded HasOldNameMethod methodUser;
	}
	
	/** */
	@Cached
	static class HasEmbeddedArray
	{
		@Id Long id;
		@OldName("oldFieldUsers") @Embedded HasOldNameField[] fieldUsers;
		@OldName("oldMethodUsers") @Embedded HasOldNameMethod[] methodUsers;
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
	public void testSimpleOldName() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(HasOldNames.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		Key<HasOldNames> key = this.fact.rawKeyToTypedKey(ent.getKey());
		HasOldNames fetched = ofy.get(key);
		
		assert fetched.getStuff().equals("oldStuff");
		assert fetched.getOtherStuff() == null;
	}
	
	/** */
	@Test
	public void testOldNameDuplicateError() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(HasOldNames.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds.put(ent);
		
		try
		{
			Key<HasOldNames> key = this.fact.rawKeyToTypedKey(ent.getKey());
			ofy.get(key);
			assert false: "Shouldn't be able to read data duplicated with @OldName";
		}
		catch (Exception ex) {}
	}

	/** */
	@Test
	public void testOldNameMethods() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(HasOldNames.class));
		ent.setProperty("weirdStuff", "5");
		ds.put(ent);
		
		Key<HasOldNames> key = this.fact.rawKeyToTypedKey(ent.getKey());
		HasOldNames fetched = ofy.get(key);
		
		assert fetched.getWeird() == 5;
	}
	
	/** */
	@Test
	public void testEasyHasEmbedded() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(HasEmbedded.class));
		ent.setProperty("fieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("methodUser.oldFoo", TEST_VALUE);
		ds.put(ent);
		
		Key<HasEmbedded> key = this.fact.rawKeyToTypedKey(ent.getKey());
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
		
		Entity ent = new Entity(this.fact.getKind(HasEmbedded.class));
		ent.setProperty("oldFieldUser.oldFoo", TEST_VALUE);
		ent.setProperty("oldMethodUser.oldFoo", TEST_VALUE);
		ds.put(ent);
		
		Key<HasEmbedded> key = this.fact.rawKeyToTypedKey(ent.getKey());
		HasEmbedded fetched = ofy.get(key);
		
		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testEasyHasEmbeddedArrau() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(this.fact.getKind(HasEmbeddedArray.class));
		ent.setProperty("fieldUsers.oldFoo", values);
		ent.setProperty("methodUsers.oldFoo", values);
		ds.put(ent);
		
		Key<HasEmbeddedArray> key = this.fact.rawKeyToTypedKey(ent.getKey());
		HasEmbeddedArray fetched = ofy.get(key);
		
		HasOldNameField[] expectedFieldUsers = new HasOldNameField[] { new HasOldNameField(TEST_VALUE), new HasOldNameField(TEST_VALUE) };
		HasOldNameMethod[] expectedMethodUsers = new HasOldNameMethod[] { new HasOldNameMethod(TEST_VALUE), new HasOldNameMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}

	/** */
	@Test
	public void testHarderHasEmbeddedArrau() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		List<String> values = new ArrayList<String>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);
		
		Entity ent = new Entity(this.fact.getKind(HasEmbeddedArray.class));
		ent.setProperty("oldFieldUsers.oldFoo", values);
		ent.setProperty("oldMethodUsers.oldFoo", values);
		ds.put(ent);
		
		Key<HasEmbeddedArray> key = this.fact.rawKeyToTypedKey(ent.getKey());
		HasEmbeddedArray fetched = ofy.get(key);
		
		HasOldNameField[] expectedFieldUsers = new HasOldNameField[] { new HasOldNameField(TEST_VALUE), new HasOldNameField(TEST_VALUE) };
		HasOldNameMethod[] expectedMethodUsers = new HasOldNameMethod[] { new HasOldNameMethod(TEST_VALUE), new HasOldNameMethod(TEST_VALUE) };
			
		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}
}