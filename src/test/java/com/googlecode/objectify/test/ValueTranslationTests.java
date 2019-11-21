/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;
import com.googlecode.objectify.impl.translate.opt.BigDecimalLongTranslatorFactory;
import com.googlecode.objectify.test.entity.Name;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.net.URL;
import java.time.Instant;
import java.util.Date;
import java.util.TimeZone;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

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
		StringBuilder bld = new StringBuilder(1500 + 1);
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

		final FullEntity<?> ent = makeEntity(HasString.class)
				.set("string", 2)    // setting a number
				.build();

		final Entity completeEntity = datastore().put(ent);

		final Key<HasString> key = Key.create(completeEntity.getKey());
		final HasString fetched = ofy().load().key(key).now();

		assertThat(fetched.string).isEqualTo("2");	// should be a string
	}

	/**
	 * Strings can be converted to numbers
	 */
	@Test
	void stringToNumber() throws Exception {
		factory().register(HasNumber.class);

		final FullEntity<?> ent = makeEntity(HasNumber.class)
				.set("number", "2")    // setting a string
				.build();

		final Entity completeEntity = datastore().put(ent);

		final Key<HasNumber> key = Key.create(completeEntity.getKey());
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

	/** For testUtilDateConversion() */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class HasUtilDate {
		@Id Long id;
		Date when;
	}

	/** */
	@Test
	void testUtilDateConversion() throws Exception {
		factory().register(HasUtilDate.class);

		final HasUtilDate hasDate = new HasUtilDate();
		hasDate.when = new Date();

		final HasUtilDate fetched = saveClearLoad(hasDate);

		assertThat(fetched).isEqualTo(hasDate);
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
	void arbitraryTranslatorIsRegistered() throws Exception {
		factory().getTranslators().add(new SimpleTranslatorFactory<BigDecimal, String>(BigDecimal.class, ValueType.STRING) {
			@Override
			protected BigDecimal toPojo(final Value<String> value) {
				return new BigDecimal(value.get());
			}

			@Override
			protected Value<String> toDatastore(final BigDecimal value) {
				return StringValue.of(value.toString());
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

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Data
	private static class HasInstant {
		@Id Long id;
		Instant instant;
	}

	/** */
	@Test
	void javaTimeInstantWorks() throws Exception {
		factory().register(HasInstant.class);

		final HasInstant hi = new HasInstant();
		hi.instant = Instant.now();

		final HasInstant fetched = saveClearLoad(hi);
		assertThat(fetched).isEqualTo(hi);
	}
}