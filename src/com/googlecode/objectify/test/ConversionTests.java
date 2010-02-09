/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.util.Arrays;

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
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.entity.Trivial;

/**
 * Tests of type conversions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ConversionTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = LoggerFactory.getLogger(ConversionTests.class);
	
	/** Used for some of the tests here */
	@Cached
	public static class HasStringArray
	{
		public @Id Long id;
		public String[] strings;
	}
	
	/** */
	@Cached
	public static class HasNames
	{
		public @Id Long id;
		public @Embedded Name[] names;
	}
	
	/** */
	public final static String BIG_STRING;
	static {
		StringBuilder bld = new StringBuilder(501);
		for (int i=0; i<501; i++)
			bld.append('z');
		
		BIG_STRING = bld.toString();
	}
	
	/**
	 * Register our local test identity
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();
		this.fact.register(HasStringArray.class);
		this.fact.register(HasNames.class);
	}

	/**
	 * Anything can be converted to a String
	 */
	@Test
	public void testStringConversion() throws Exception
	{
		Objectify ofy = this.fact.begin();
		DatastoreService ds = ofy.getDatastore();
		
		Entity ent = new Entity(this.fact.getKind(Trivial.class));
		ent.setProperty("someNumber", 1);
		ent.setProperty("someString", 2);	// setting a number
		ds.put(ent);
		
		Key<Trivial> key = this.fact.rawKeyToTypedKey(ent.getKey());
		Trivial fetched = ofy.get(key);
		
		assert fetched.getSomeNumber() == 1;
		assert fetched.getSomeString().equals("2");	// should be a string
	}
	
	/**
	 * Strings greater than 500 chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	public void testBigStrings() throws Exception
	{
		Trivial triv = new Trivial(BIG_STRING, 0);
		Trivial fetched = this.putAndGet(triv);
		
		assert fetched.getSomeString().equals(BIG_STRING);
	}

	/**
	 * Strings greater than 500 chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	public void testBigStringsInCollections() throws Exception
	{
		HasStringArray has = new HasStringArray();
		has.strings = new String[] { "Short", BIG_STRING, "AlsoShort" };
		
		HasStringArray fetched = this.putAndGet(has);
		
		// Nasty behavior!
		assert !Arrays.equals(fetched.strings, has.strings);
		assert Arrays.equals(fetched.strings, new String[] { "Short", "AlsoShort", BIG_STRING });
	}

	/**
	 * You should not be able to store a big string in an embedded collection
	 */
	@Test
	public void testBigStringsInEmbeddedCollections() throws Exception
	{
		HasNames has = new HasNames();
		has.names = new Name[] { new Name("Bob", BIG_STRING) };
		
		Objectify ofy = this.fact.begin();
		try
		{
			ofy.put(has);
			assert false : "You should not be able to put() embedded collections with big strings"; 
		}
		catch (IllegalStateException ex)
		{
			// Correct
		}
	}
}