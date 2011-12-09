/*
 */

package com.googlecode.objectify.test;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.testng.annotations.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * Tests of type conversions.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ValueTranslationTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(ValueTranslationTests.class.getName());
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasString
	{
		public @Id Long id;
		public String string;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasNumber
	{
		public @Id Long id;
		public int number;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasStringArray
	{
		public @Id Long id;
		public String[] strings;
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasNames
	{
		public @Id Long id;
		public @Embed Name[] names;
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
	 * Anything can be converted to a String
	 */
	@Test
	public void numberToString() throws Exception
	{
		fact.register(HasString.class);
		
		TestObjectify ofy = this.fact.begin();
		DatastoreService ds = ds();
		
		Entity ent = new Entity(Key.getKind(HasString.class));
		ent.setProperty("string", 2);	// setting a number
		ds.put(null, ent);
		
		Key<HasString> key = Key.create(ent.getKey());
		HasString fetched = ofy.load().key(key).get();
		
		assert fetched.string.equals("2");	// should be a string
	}
	
	/**
	 * Strings can be converted to numbers
	 */
	@Test
	public void stringToNumber() throws Exception
	{
		fact.register(HasNumber.class);
		
		TestObjectify ofy = this.fact.begin();
		DatastoreService ds = ds();
		
		Entity ent = new Entity(Key.getKind(HasNumber.class));
		ent.setProperty("number", "2");	// setting a string
		ds.put(null, ent);
		
		Key<HasNumber> key = Key.create(ent.getKey());
		HasNumber fetched = ofy.load().key(key).get();
		
		assert fetched.number == 2;	// should be a number
	}
	
	/**
	 * Strings greater than 500 chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	public void testBigStrings() throws Exception
	{
		this.fact.register(HasString.class);
		
		HasString has = new HasString();
		has.string = BIG_STRING;
		HasString fetched = this.putClearGet(has);
		
		assert fetched.string.equals(BIG_STRING);
	}

	/**
	 * Strings greater than 500 chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	public void testBigStringsInCollections() throws Exception
	{
		this.fact.register(HasStringArray.class);

		HasStringArray has = new HasStringArray();
		has.strings = new String[] { "Short", BIG_STRING, "AlsoShort" };
		
		@SuppressWarnings("unused")
		HasStringArray fetched = this.putClearGet(has);

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
		fact.register(HasNames.class);
		
		HasNames has = new HasNames();
		has.names = new Name[] { new Name("Bob", BIG_STRING) };
		
		TestObjectify ofy = this.fact.begin();
		try {
			ofy.save().entity(has).now();
			assert false : "You should not be able to put() embedded collections with big strings"; 
		}
		catch (SaveException ex) {
			// Correct
		}
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
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
		
		Blobby c = this.putClearGet(b);
		
		assert Arrays.equals(b.stuff, c.stuff);
	}
	
	/** For testSqlDateConversion() */
	@com.googlecode.objectify.annotation.Entity
	@Cache
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
		
		HasSqlDate fetched = this.putClearGet(hasDate);
		
		assert hasDate.when.equals(fetched.when);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class HasBigDecimal
	{
		public @Id Long id;
		public BigDecimal data;
	}
	
	/** Make sure we can't execute without converter registration */
	@Test
	public void testAddedConversion1() throws Exception
	{
		this.fact.register(HasBigDecimal.class);

		HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);
		
		try
		{
			this.putClearGet(hbd);
			assert false;	// shouldn't be possible without registering converter
		}
		catch (SaveException ex) {}
	}

	/** Make sure we can execute with converter registration */
	@Test
	public void testAddedConversion2() throws Exception
	{
		this.fact.getTranslators().add(new ValueTranslatorFactory<BigDecimal, String>(BigDecimal.class) {
			@Override
			protected ValueTranslator<BigDecimal, String> createSafe(Path path, Property property, Type type, CreateContext ctx) {
				return new ValueTranslator<BigDecimal, String>(path, String.class) {
					@Override
					protected BigDecimal loadValue(String value, LoadContext ctx) {
						return new BigDecimal(value);
					}

					@Override
					protected String saveValue(BigDecimal value, SaveContext ctx) {
						return value.toString();
					}
				};
			}
		});
		
		this.fact.register(HasBigDecimal.class);

		HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);
		
		HasBigDecimal fetched = this.putClearGet(hbd);
		assert hbd.data.equals(fetched.data);
	}

	/** */
	@Test
	public void testBigDecimalLongConverter() throws Exception
	{
		this.fact.getTranslators().add(new BigDecimalLongTranslatorFactory());
		this.fact.register(HasBigDecimal.class);

		HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);
		
		HasBigDecimal fetched = this.putClearGet(hbd);
		assert hbd.data.equals(fetched.data);
	}
	
	@com.googlecode.objectify.annotation.Entity
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

		HasTimeZone fetched = this.putClearGet(htz);
		assert htz.tz.equals(fetched.tz);
	}
}