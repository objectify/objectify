/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.ShortBlob;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of type conversions.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class ValueTranslationTests extends TestBase {

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasString {
		@Id Long id;
		String string;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Index
	@Data
	private static class HasStringIndexInversion {
		@Id Long id;
		@Unindex String string;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasNumber {
		@Id Long id;
		int number;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasStringArray {
		@Id Long id;
		String[] strings;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasNames {
		@Id Long id;
		Name[] names;
	}

	/** */
	private final static String BIG_STRING;
	static {
		StringBuilder bld = new StringBuilder(DataTypeUtils.MAX_STRING_PROPERTY_LENGTH + 1);
		for (int i=0; i<bld.capacity(); i++)
			bld.append('\u2202');

		BIG_STRING = bld.toString();
	}

	/**
	 * Anything can be converted to a String
	 */
	@Test
	void numberToString() throws Exception {
		factory().register(HasString.class);

		final Entity ent = new Entity(Key.getKind(HasString.class));
		ent.setProperty("string", 2);	// setting a number
		ds().put(null, ent);

		final Key<HasString> key = Key.create(ent.getKey());
		final HasString fetched = ofy().load().key(key).now();

		assertThat(fetched.string).isEqualTo("2");	// should be a string
	}

	/**
	 * Strings can be converted to numbers
	 */
	@Test
	void stringToNumber() throws Exception {
		factory().register(HasNumber.class);

		final DatastoreService ds = ds();

		final Entity ent = new Entity(Key.getKind(HasNumber.class));
		ent.setProperty("number", "2");	// setting a string
		ds.put(null, ent);

		final Key<HasNumber> key = Key.create(ent.getKey());
		final HasNumber fetched = ofy().load().key(key).now();

		assertThat(fetched.number).isEqualTo(2);	// should be a number
	}

	/**
	 * Strings greater than some number of chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	void bigStringsAreConvertedToText() throws Exception {
		factory().register(HasString.class);

		final HasString has = new HasString();
		has.string = BIG_STRING;
		final HasString fetched = saveClearLoad(has);

		assertThat(fetched).isEqualTo(has);
	}

	/**
	 * Strings greater than some number of chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	void bigStringsInCollectionsAreConvertedToText() throws Exception {
		factory().register(HasStringArray.class);

		final HasStringArray has = new HasStringArray();
		has.strings = new String[] { "Short", BIG_STRING, "AlsoShort" };

		@SuppressWarnings("unused")
		final HasStringArray fetched = saveClearLoad(has);

		// When caching is enabled you get the same order back, but if caching is disabled,
		// you get the Text moved to the end of the heterogenous collection.  Ick.
		//assert !Arrays.equals(fetched.strings, has.strings);
		//assert Arrays.equals(fetched.strings, new String[] { "Short", "AlsoShort", BIG_STRING });
	}

	/**
	 * You should be able to store a big string in an embedded collection
	 */
	@Test
	void bigStringsAreAllowedInEmbeddedCollections() throws Exception {
		factory().register(HasNames.class);

		final HasNames has = new HasNames();
		has.names = new Name[] { new Name("Bob", BIG_STRING) };

		final HasNames fetched = saveClearLoad(has);
		assertThat(fetched.names[0]).isEqualTo(has.names[0]);
	}

	/**
	 * Strings greater than a certain number of chars get converted to Text and back.  This has
	 * some potentially odd effects.
	 */
	@Test
	void testBigStringsWithIndexInversion() throws Exception {
		factory().register(HasStringIndexInversion.class);

		final HasStringIndexInversion has = new HasStringIndexInversion();
		has.string = BIG_STRING;
		final HasStringIndexInversion fetched = saveClearLoad(has);

		assertThat(fetched).isEqualTo(has);
	}
	
	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasText {
		@Id Long id;
		Text text;
	}

	/**
	 * Stored Strings can be converted to Text in the data model
	 */
	@Test
	void stringsCanBeConvertedToText() throws Exception {
		factory().register(HasText.class);

		final Entity ent = new Entity(Key.getKind(HasText.class));
		ent.setProperty("text", "foo");	// setting a string
		ds().put(null, ent);

		final Key<HasText> key = Key.create(ent.getKey());
		final HasText fetched = ofy().load().key(key).now();

		assertThat(fetched.text.getValue()).isEqualTo("foo");
	}

	/**
	 * Stored numbers can be converted to Text in the data model
	 */
	@Test
	void numbersCanBeConvertedToText() throws Exception {
		factory().register(HasText.class);

		final Entity ent = new Entity(Key.getKind(HasText.class));
		ent.setProperty("text", 2);	// setting a number
		ds().put(null, ent);

		final Key<HasText> key = Key.create(ent.getKey());
		final HasText fetched = ofy().load().key(key).now();

		assertThat(fetched.text.getValue()).isEqualTo("2");
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class Blobby {
		@Id Long id;
		byte[] stuff;
	}

	/** */
	@Test
	void testBlobConversion() throws Exception {
		factory().register(Blobby.class);

		final Blobby b = new Blobby();
		b.stuff = new byte[] { 1, 2, 3 };

		final Blobby c = saveClearLoad(b);

		assertThat(c.stuff).isEqualTo(b.stuff);
	}

	/** */
	@Test
	void shortBlobsAreConvertedToByteArrays() throws Exception {
		factory().register(Blobby.class);

		final Entity ent = new Entity("Blobby");
		final ShortBlob shortBlob = new ShortBlob(new byte[]{1, 2, 3});
		ent.setProperty("stuff", shortBlob);

		final Key<Blobby> blobbyKey = Key.create(ds().put(ent));

		final Blobby blobby = ofy().load().key(blobbyKey).safe();

		assertThat(blobby.stuff).isEqualTo(shortBlob.getBytes());
	}

	/** For testSqlDateConversion() */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasSqlDate {
		@Id Long id;
		java.sql.Date when;
	}

	/** */
	@Test
	void testSqlDateConversion() throws Exception {
		factory().register(HasSqlDate.class);

		final HasSqlDate hasDate = new HasSqlDate();
		hasDate.when = new java.sql.Date(System.currentTimeMillis());

		final HasSqlDate fetched = saveClearLoad(hasDate);

		assertThat(fetched).isEqualTo(hasDate);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasBigDecimal {
		@Id Long id;
		BigDecimal data;
	}

	@Test
	void translatorIsNeededForUnknownType() throws Exception {
		factory().register(HasBigDecimal.class);

		final HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);

		assertThrows(SaveException.class, () -> saveClearLoad(hbd));
	}

	@Test
	void arbitraryTranslatorIsRegistered() throws Exception {
		factory().getTranslators().add(new ValueTranslatorFactory<BigDecimal, String>(BigDecimal.class) {
			@Override
			protected ValueTranslator<BigDecimal, String> createValueTranslator(TypeKey tk, CreateContext ctx, Path path) {
				return new ValueTranslator<BigDecimal, String>(String.class) {
					@Override
					protected BigDecimal loadValue(String value, LoadContext ctx, Path path) throws SkipException {
						return new BigDecimal(value);
					}

					@Override
					protected String saveValue(BigDecimal value, boolean index, SaveContext ctx, Path path) throws SkipException {
						return value.toString();
					}
				};
			}
		});

		factory().register(HasBigDecimal.class);

		final HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);

		final HasBigDecimal fetched = saveClearLoad(hbd);
		assertThat(fetched).isEqualTo(hbd);
	}

	/** */
	@Test
	void testBigDecimalLongTranslator() throws Exception {
		factory().getTranslators().add(new BigDecimalLongTranslatorFactory());
		factory().register(HasBigDecimal.class);

		final HasBigDecimal hbd = new HasBigDecimal();
		hbd.data = new BigDecimal(32.25);

		final HasBigDecimal fetched = saveClearLoad(hbd);
		assertThat(fetched).isEqualTo(hbd);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	private static class HasTimeZone {
		@Id Long id;
		TimeZone tz;
	}

	/** */
	@Test
	void testTimeZoneTranslator() throws Exception {
		factory().register(HasTimeZone.class);

		final HasTimeZone htz = new HasTimeZone();
		htz.tz = TimeZone.getDefault();

		final HasTimeZone fetched = saveClearLoad(htz);
		assertThat(fetched).isEqualTo(htz);
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	private static class HasURL {
		@Id Long id;
		URL url;
	}

	/** */
	@Test
	void testURLTranslator() throws Exception {
		factory().register(HasURL.class);

		final HasURL hu = new HasURL();
		hu.url = new URL("http://example.com/foo?bar=baz");

		final HasURL fetched = saveClearLoad(hu);
		assertThat(fetched).isEqualTo(hu);
	}
}