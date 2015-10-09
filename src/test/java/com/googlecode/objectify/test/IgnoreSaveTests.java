/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.condition.IfDefault;
import com.googlecode.objectify.condition.IfTrue;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Tests of using the @IgnoreSave annotation and its various conditions.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IgnoreSaveTests extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(IgnoreSaveTests.class.getName());

	/** */
	public static final String TEST_VALUE = "blah";

	/** Just making sure it works when we have deeper inheritance */
	static class DeeperIfTrue extends IfTrue {}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class CompletelyUnsaved
	{
		@Id Long id;
		@IgnoreSave String foo;
	}

	/** */
	@Test
	public void testCompletelyUnsaved() throws Exception
	{
		fact().register(CompletelyUnsaved.class);

		Entity ent = new Entity(Key.getKind(CompletelyUnsaved.class));
		ent.setProperty("foo", TEST_VALUE);
		ds().put(null, ent);

		Key<CompletelyUnsaved> key = Key.create(ent.getKey());
		CompletelyUnsaved fetched = ofy().load().key(key).now();
		assert fetched.foo.equals(TEST_VALUE);

		fetched = ofy().saveClearLoad(fetched);
		assert fetched.foo == null;	// this would fail without the session clear()
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class UnsavedWhenTrue
	{
		@Id Long id;
		@IgnoreSave(IfTrue.class) boolean foo;
		boolean bar;
	}

	/** */
	@Test
	public void testUnsavedWhenTrue() throws Exception
	{
		fact().register(UnsavedWhenTrue.class);

		UnsavedWhenTrue thing = new UnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;

		UnsavedWhenTrue fetched = ofy().saveClearLoad(thing);
		assert !fetched.foo;	// would fail without the session clear()
		assert fetched.bar;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class DeeperUnsavedWhenTrue
	{
		@Id Long id;
		@IgnoreSave(IfTrue.class) boolean foo;
		boolean bar;
	}

	/** */
	@Test
	public void testDeeperUnsavedWhenTrue() throws Exception
	{
		fact().register(DeeperUnsavedWhenTrue.class);

		DeeperUnsavedWhenTrue thing = new DeeperUnsavedWhenTrue();
		thing.foo = true;
		thing.bar = true;

		DeeperUnsavedWhenTrue fetched = ofy().saveClearLoad(thing);
		assert !fetched.foo;	// would fail without the session clear()
		assert fetched.bar;
	}

	/** Should not be registerable */
	@com.googlecode.objectify.annotation.Entity
	static class BadFieldType
	{
		@Id Long id;
		@IgnoreSave(IfTrue.class) String foo;
	}

	/** Should not be registerable */
	@com.googlecode.objectify.annotation.Entity
	static class DeeperBadFieldType
	{
		@Id Long id;
		@IgnoreSave(DeeperIfTrue.class) String foo;
	}

	/** */
	@Test
	public void testBadFieldTypeNotRegisterable() throws Exception
	{
		try {
			fact().register(BadFieldType.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
	}

	/** */
	@Test
	public void testDeeperBadFieldTypeNotRegisterable() throws Exception
	{
		try {
			fact().register(DeeperBadFieldType.class);
			assert false;
		}
		catch (IllegalStateException ex) {}
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class UnsavedDefaults
	{
		@Id Long id;
		@IgnoreSave(IfDefault.class) boolean booleanDefault = true;
		@IgnoreSave(IfDefault.class) String stringDefault = TEST_VALUE;
		@IgnoreSave(IfDefault.class) int intDefault = 10;
		@IgnoreSave(IfDefault.class) float floatDefault = 10f;
	}

	/** */
	@Test
	public void testUnsavedDefaults() throws Exception
	{
		fact().register(UnsavedDefaults.class);

		UnsavedDefaults thing = new UnsavedDefaults();
		Key<UnsavedDefaults> key = ofy().save().entity(thing).now();

		// Now get the raw entity and verify that it doesn't have properties saved
		Entity ent = ds().get(null, key.getRaw());
		assert ent.getProperties().isEmpty();
	}
}
