/*
 */

package com.googlecode.objectify.test;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.condition.IfDefault;
import com.googlecode.objectify.condition.IfTrue;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests of using the @IgnoreSave annotation and its various conditions.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class IgnoreSaveTests extends TestBase {
	/** */
	private static final String TEST_VALUE = "blah";

	/** Just making sure it works when we have deeper inheritance */
	private static class DeeperIfTrue extends IfTrue {}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class CompletelyUnsaved {
		@Id Long id;
		@IgnoreSave String foo;
	}

	/** */
	@Test
	void ignoreSavePropertiesAreIgnoredOnSave() throws Exception {
		factory().register(CompletelyUnsaved.class);

		final FullEntity<?> ent = makeEntity(CompletelyUnsaved.class)
				.set("foo", TEST_VALUE)
				.build();

		final Entity complete = datastore().put(ent);

		final Key<CompletelyUnsaved> key = Key.create(complete.getKey());
		final CompletelyUnsaved fetched = ofy().load().key(key).now();
		assertThat(fetched.foo).isEqualTo(TEST_VALUE);

		final CompletelyUnsaved fetched2 = saveClearLoad(fetched);
		assertThat(fetched2.foo).isNull();	// this would fail without the session clear()
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class UnsavedWhenTrue {
		@Id Long id;
		@IgnoreSave(IfTrue.class) boolean foo;
		boolean bar;
	}

	/** */
	@Test
	void conditionalIgnoreSave() throws Exception {
		factory().register(UnsavedWhenTrue.class);

		final UnsavedWhenTrue thing = new UnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;

		final UnsavedWhenTrue fetched = saveClearLoad(thing);
		assertThat(fetched.foo).isFalse();	// would fail without the session clear()
		assertThat(fetched.bar).isTrue();
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class DeeperUnsavedWhenTrue {
		@Id Long id;
		@IgnoreSave(DeeperIfTrue.class) boolean foo;
		boolean bar;
	}

	/** */
	@Test
	void subclassedConditionalIgnoreSave() throws Exception {
		factory().register(DeeperUnsavedWhenTrue.class);

		DeeperUnsavedWhenTrue thing = new DeeperUnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;

		DeeperUnsavedWhenTrue fetched = saveClearLoad(thing);
		assertThat(fetched.foo).isFalse();	// would fail without the session clear()
		assertThat(fetched.bar).isTrue();
	}

	/** Should not be registerable */
	@com.googlecode.objectify.annotation.Entity
	private static class BadFieldType {
		@Id Long id;
		@IgnoreSave(IfTrue.class) String foo;
	}

	/** Should not be registerable */
	@com.googlecode.objectify.annotation.Entity
	private static class DeeperBadFieldType {
		@Id Long id;
		@IgnoreSave(DeeperIfTrue.class) String foo;
	}

	/** */
	@Test
	void badFieldTypeNotRegisterable() throws Exception {
		assertThrows(IllegalStateException.class, () -> factory().register(BadFieldType.class));
	}

	/** */
	@Test
	void deeperBadFieldTypeNotRegisterable() throws Exception {
		assertThrows(IllegalStateException.class, () -> factory().register(DeeperBadFieldType.class));
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	@Data
	private static class UnsavedDefaults {
		@Id Long id;
		@IgnoreSave(IfDefault.class) boolean booleanDefault = true;
		@IgnoreSave(IfDefault.class) String stringDefault = TEST_VALUE;
		@IgnoreSave(IfDefault.class) int intDefault = 10;
		@IgnoreSave(IfDefault.class) float floatDefault = 10f;
	}

	/** */
	@Test
	void unsavedIfDefault() throws Exception {
		factory().register(UnsavedDefaults.class);

		final UnsavedDefaults thing = new UnsavedDefaults();
		final Key<UnsavedDefaults> key = ofy().save().entity(thing).now();

		// Now get the raw entity and verify that it doesn't have properties saved
		final Entity ent = datastore().get(key.getRaw());
		assertThat(ent.getNames()).isEmpty();
	}

}