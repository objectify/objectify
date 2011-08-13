/*
 * $Id: BeanMixin.java 1075 2009-05-07 06:41:19Z lhoriman $
 * $URL: https://subetha.googlecode.com/svn/branches/resin/rtest/src/org/subethamail/rtest/util/BeanMixin.java $
 */

package com.googlecode.objectify.test;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.logging.Logger;

import javax.persistence.Embedded;
import javax.persistence.Id;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.impl.conv.BigDecimalLongConverter;
import com.googlecode.objectify.impl.conv.Converter;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
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
	private static Logger log = Logger.getLogger(ConversionTests.class.getName());
	
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
		
		Entity ent = new Entity(Key.getKind(Trivial.class));
		ent.setProperty("someNumber", 1);
		ent.setProperty("someString", 2);	// setting a number
		ds.put(ent);
		
		Key<Trivial> key = new Key<Trivial>(ent.getKey());
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
		
		@SuppressWarnings("unused")
		HasStringArray fetched = this.putAndGet(has);

		// When caching is enabled you get the same order back, but if caching is disabled,
		// you get the Text moved to the end of the heterogenous collection.  Ick.
		//assert !Arrays.equals(fetched.strings, has.strings);
		//assert Arrays.equals(fetched.strings, new String[] { "Short", "AlsoShort", BIG_STRING });
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
	
	/** */
	@Cached
	public static class Blobby
	{
		public @Id Long id;
		public byte[] stuff;
	}
	
	/** */
	@Test
	public void testBlobConversion() throws Exception
	{
		this.fact.register(Blobby.class);

		Blobby b = new Blobby();
		b.stuff = new byte[] { 1, 2, 3 };
		
		Blobby c = this.putAndGet(b);
		
		assert Arrays.equals(b.stuff, c.stuff);
	}
	
	/** For testSqlDateConversion() */
	@Cached
	public static class HasSqlDate
	{
		public @Id Long id;
		public java.sql.Date when;
	}
	
	/** */
	@Test
	public void testSqlDateConversion() throws Exception
	{
		this.fact.register(HasSqlDate.class);

		HasSqlDate hasDate = new HasSqlDate();
		hasDate.when = new java.sql.Date(System.currentTimeMillis());
		
		HasSqlDate fetched = this.putAndGet(hasDate);
		
		assert hasDate.when.equals(fetched.when);
	}

	/** */
	@Cached
	public static class HasBigDecimal
	{
		public @Id Long id;
		public BigDecimal data;
	}
	
	/** */
	@Test
	public void testAddedConversion() throws Exception
	{
		this.fact.register(HasBigDecimal.class);

		HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);
		
		try
		{
			this.putAndGet(hbd);
			assert false;	// shouldn't be possible without registering converter
		}
		catch (IllegalArgumentException ex) {}
		
		this.fact.getConversions().add(new Converter() {
			@Override
			public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
			{
				if (fieldType == BigDecimal.class && value instanceof String)
					return new BigDecimal((String)value);
				else
					return null;
			}
			
			@Override
			public Object forDatastore(Object value, ConverterSaveContext ctx)
			{
				if (value instanceof BigDecimal)
					return value.toString();
				else
					return null;
			}
		});
		
		HasBigDecimal fetched = this.putAndGet(hbd);
		assert hbd.data.equals(fetched.data);
	}

	/** */
	@Test
	public void testBigDecimalLongConverter() throws Exception
	{
		this.fact.register(HasBigDecimal.class);

		HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);
		
		this.fact.getConversions().add(new BigDecimalLongConverter());
		
		HasBigDecimal fetched = this.putAndGet(hbd);
		assert hbd.data.equals(fetched.data);
	}
	
	public static class HasTimeZone
	{
		public @Id Long id;
		public TimeZone tz;
	}

	/** */
	@Test
	public void testTimeZoneConverter() throws Exception
	{
		this.fact.register(HasTimeZone.class);

		HasTimeZone htz = new HasTimeZone();
		htz.tz = TimeZone.getDefault();

		HasTimeZone fetched = this.putAndGet(htz);
		assert htz.tz.equals(fetched.tz);
	}
}