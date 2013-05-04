/*
 */

package com.googlecode.objectify.test;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.TranslateException;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.IgnoreLoad;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * More tests of using the @AlsoLoad annotation
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AlsoLoadTests2 extends TestBase
{
	/** */
	@SuppressWarnings("unused")
	private static Logger log = Logger.getLogger(AlsoLoadTests2.class.getName());

	/** */
	public static final String TEST_VALUE = "blah";

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class MethodOverridesField
	{
		@Id Long id;
		@IgnoreLoad String foo;
		String bar;
		public void set(@AlsoLoad("foo") String overrides)
		{
			this.bar = overrides;
		}
	}

	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUp()
	{
		super.setUp();

		fact().register(MethodOverridesField.class);
	}

	/** */
	@Test
	public void testMethodOverridingField() throws Exception
	{
		com.google.appengine.api.datastore.Entity ent = new com.google.appengine.api.datastore.Entity(Key.getKind(MethodOverridesField.class));
		ent.setProperty("foo", TEST_VALUE);
		ds().put(ent);

		Key<MethodOverridesField> key = Key.create(ent.getKey());
		MethodOverridesField fetched = ofy().load().key(key).now();

		assert fetched.foo == null;
		assert fetched.bar.equals(TEST_VALUE);
	}

	@com.googlecode.objectify.annotation.Entity
	public static class HasMap
	{
		@Id
		Long id;
		@AlsoLoad("alsoPrimitives")
		Map<String, Long> primitives = new HashMap<String, Long>();
	}

	@Test
	public void testAlsoLoadMap() throws Exception
	{
		fact().register(HasMap.class);

		Entity ent = new Entity(Key.getKind(HasMap.class));
		ent.setProperty("alsoPrimitives.one", 1L);
		ent.setProperty("primitives.two", 2L);
		ds().put(ent);

		Key<HasMap> key = Key.create(ent.getKey());

		try {
			ofy().load().key(key).now();
			assert false;
		} catch (TranslateException ex) {
			// couldn't load conflicting values
		}
	}
}