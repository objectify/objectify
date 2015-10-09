/*
 */

package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

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
	static class HasAlsoLoadField {
		@AlsoLoad("oldFoo") String foo;

		public HasAlsoLoadField() {}
		public HasAlsoLoadField(String value) { this.foo = value; }

		public boolean equals(Object other)
		{
			return this.foo.equals(((HasAlsoLoadField)other).foo);
		}
	}

	/** */
	static class HasAlsoLoadMethod {
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
	static class HasEmbedded {
		@Id Long id;
		@AlsoLoad("oldFieldUser") HasAlsoLoadField fieldUser;
		@AlsoLoad("oldMethodUser") HasAlsoLoadMethod methodUser;
	}

	/** */
	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class HasEmbeddedArray {
		@Id Long id;
		@AlsoLoad("oldFieldUsers") HasAlsoLoadField[] fieldUsers;
		@AlsoLoad("oldMethodUsers") HasAlsoLoadMethod[] methodUsers;
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	static class HasAlsoLoads {
		@Id Long id;
		public Long getId() { return this.id; }
		public void setId(Long value) { this.id = value; }

		@AlsoLoad("oldStuff")
		String stuff;
		public String getStuff() { return this.stuff; }
		public void setStuff(String value) { this.stuff = value; }

		@AlsoLoad("oldOtherStuff")
		String otherStuff;
		public String getOtherStuff() { return this.otherStuff; }
		public void setOtherStuff(String value) { this.otherStuff = value; }

		/** Tests loading with @AlsoLoad on a method */
		@Ignore Integer weird;
		public Integer getWeird() { return this.weird; }
		void namedAnything(@AlsoLoad("weirdStuff") String stuff)
		{
			this.weird = Integer.valueOf(stuff);
		}

		/** Default constructor must always exist */
		public HasAlsoLoads() {}

		public HasAlsoLoads(String stuff, String otherStuff) {
			this.stuff = stuff;
			this.otherStuff = otherStuff;
		}
	}

	/**
	 * Add an entry to the database that should never come back from null queries.
	 */
	@BeforeMethod
	public void setUpExtra() {
		fact().register(HasAlsoLoads.class);
		fact().register(HasEmbedded.class);
		fact().register(HasEmbeddedArray.class);
	}

	/** */
	@Test
	public void testSimpleAlsoLoad() throws Exception {
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);

		Key<HasAlsoLoads> key = Key.create(ent.getKey());
		HasAlsoLoads fetched = ofy().load().key(key).now();

		assert fetched.getStuff().equals("oldStuff");
		assert fetched.getOtherStuff() == null;
	}

	/** */
	@Test
	public void testAlsoLoadDuplicateError() throws Exception {
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("stuff", "stuff");
		ent.setProperty("oldStuff", "oldStuff");
		ds().put(ent);

		try {
			Key<HasAlsoLoads> key = Key.create(ent.getKey());
			ofy().load().key(key).now();
			assert false: "Shouldn't be able to read data duplicated with @AlsoLoad";
		}
		catch (Exception ex) {}
	}

	/** */
	@Test
	public void testAlsoLoadMethods() throws Exception {
		Entity ent = new Entity(Key.getKind(HasAlsoLoads.class));
		ent.setProperty("weirdStuff", "5");
		ds().put(ent);

		Key<HasAlsoLoads> key = Key.create(ent.getKey());
		HasAlsoLoads fetched = ofy().load().key(key).now();

		assert fetched.getWeird() == 5;
	}

	/** */
	@Test
	public void testEasyHasEmbedded() throws Exception {
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("fieldUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("methodUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ds().put(ent);

		Key<HasEmbedded> key = Key.create(ent.getKey());
		HasEmbedded fetched = ofy().load().key(key).now();

		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testHarderHasEmbedded() throws Exception {
		Entity ent = new Entity(Key.getKind(HasEmbedded.class));
		ent.setProperty("oldFieldUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("oldMethodUser", makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ds().put(ent);

		Key<HasEmbedded> key = Key.create(ent.getKey());
		HasEmbedded fetched = ofy().load().key(key).now();

		assert TEST_VALUE.equals(fetched.fieldUser.foo);
		assert TEST_VALUE.equals(fetched.methodUser.foo);
	}

	/** */
	@Test
	public void testEasyHasEmbeddedArray() throws Exception {
		List<String> values = new ArrayList<>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);

		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		List<EmbeddedEntity> list = new ArrayList<>();
		list.add(makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		list.add(makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("fieldUsers", list);
		ent.setProperty("methodUsers", list);
		ds().put(ent);

		Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		HasEmbeddedArray fetched = ofy().load().key(key).now();

		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };

		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}

	/** */
	@Test
	public void testHarderHasEmbeddedArray() throws Exception {
		List<String> values = new ArrayList<>();
		values.add(TEST_VALUE);
		values.add(TEST_VALUE);

		Entity ent = new Entity(Key.getKind(HasEmbeddedArray.class));
		List<EmbeddedEntity> list = new ArrayList<>();
		list.add(makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		list.add(makeEmbeddedEntityWithProperty("oldFoo", TEST_VALUE));
		ent.setProperty("oldFieldUsers", list);
		ent.setProperty("oldMethodUsers", list);
		ds().put(ent);

		Key<HasEmbeddedArray> key = Key.create(ent.getKey());
		HasEmbeddedArray fetched = ofy().load().key(key).now();

		HasAlsoLoadField[] expectedFieldUsers = new HasAlsoLoadField[] { new HasAlsoLoadField(TEST_VALUE), new HasAlsoLoadField(TEST_VALUE) };
		HasAlsoLoadMethod[] expectedMethodUsers = new HasAlsoLoadMethod[] { new HasAlsoLoadMethod(TEST_VALUE), new HasAlsoLoadMethod(TEST_VALUE) };

		assert Arrays.equals(fetched.fieldUsers, expectedFieldUsers);
		assert Arrays.equals(fetched.methodUsers, expectedMethodUsers);
	}
}
