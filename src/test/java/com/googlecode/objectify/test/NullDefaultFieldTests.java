package com.googlecode.objectify.test;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.test.util.TestBase;
import org.testng.annotations.Test;

import static com.googlecode.objectify.test.util.TestObjectifyService.ds;
import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Test behavior of null fields, and default values
 */
public class NullDefaultFieldTests extends TestBase
{
	public static class Struct {
		String s = "default1";

		public Struct() {}
		public Struct(String s)
		{
			this.s = s;
		}
	}

	@com.googlecode.objectify.annotation.Entity
	@Cache
	public static class EntityWithDefault {
		@Id
		Long id;
		/** existing property */
		String a;
		/** new property */
		String b = "foo";
		/** new property */
		@Unindex
		String c = "bar";
		/** new embedded */
		Struct s = new Struct("default2");

		public EntityWithDefault()
		{
		}

		public EntityWithDefault(String a)
		{
			this.a = a;
		}

		public EntityWithDefault(String a, String b, String c)
		{
			this.a = a;
			this.b = b;
			this.c = c;
		}
	}

	/**
	 * Test that an entity in the datastore with absent fields loads correctly,
	 * when the fields in the entity class have default values
	 */
	@Test
	public void testNewVersionOfEntity() throws Exception {
		fact().register(EntityWithDefault.class);

		// e1 has absent properties
		Entity e1 = new Entity("EntityWithDefault");
		e1.setProperty("a", "1");
		com.google.appengine.api.datastore.Key k1 = ds().put(null, e1);

		EntityWithDefault o = ofy().load().type(EntityWithDefault.class).id(k1.getId()).now();

		assert o.a != null;
		assert "1".equals(o.a);
		assert o.b != null;
		assert "foo".equals(o.b);
		assert o.c != null;
		assert "bar".equals(o.c);
		assert o.s != null;
		assert "default2".equals(o.s.s);
	}

	/**
	 * Test that writing null via Objectify is preserved, even when the fields have default values
	 */
	@Test
	public void testDefaultValuesAndNull() throws Exception {
		fact().register(EntityWithDefault.class);

		Key<EntityWithDefault> k1 = ofy().save().entity(new EntityWithDefault("A")).now();
		Key<EntityWithDefault> k2 = ofy().save().entity(new EntityWithDefault("A", "B", "C")).now();
		Key<EntityWithDefault> k3 = ofy().save().entity(new EntityWithDefault("A", null, null)).now();

		EntityWithDefault o1 = ofy().load().key(k1).now();
		EntityWithDefault o2 = ofy().load().key(k2).now();
		EntityWithDefault o3 = ofy().load().key(k3).now();

		assert "A".equals(o1.a);
		assert "foo".equals(o1.b);
		assert "bar".equals(o1.c);

		assert "A".equals(o2.a);
		assert "B".equals(o2.b);
		assert "C".equals(o2.c);

		assert "A".equals(o3.a);
		assert null == o3.b;
		assert null == o3.c;
	}
}
