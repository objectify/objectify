package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
public class LoadGroupTransitiveTest extends TestBase {

	@Entity
	public static class A {
		@Id
		public long id = 1;
		@Index String foo = "foo";

		@Load(BGroup.class)
		public Ref<B> b;
	}

	@Entity
	public static class B {
		@Id
		public long id = 1;

		@Load(CGroup.class)
		public Ref<C> c;
	}

	@Entity
	public static class C {
		@Id
		public long id = 1;
	}

	public static class BGroup {}
	public static class CGroup {}

	@Test
	public void testTransitiveLoad() {
		this.fact.register(A.class);
		this.fact.register(B.class);
		this.fact.register(C.class);

		final B b = new B();
		final C c = new C();
		b.c = Ref.create(c);

		ofy().save().entities(b, c).now();

		ofy().clear();
		B b1 = ofy().load().entity(b).get();

		A a = new A();
		a.b = Ref.create(b1);	// the problem was that this created a ResultNow<?>-based Ref which was then skipped for upgrades

		ofy().save().entity(a).now();

		A a2 = ofy().load().group(BGroup.class, CGroup.class).entity(a).get();
		B b2 = a2.b.get();
		C c2 = b2.c.get();	// this used to fail with Ref<?> value has not been initialized

		assert c2 != null;
	}

	@Test
	public void testTransitiveLoadStepwise() {
		this.fact.register(A.class);
		this.fact.register(B.class);
		this.fact.register(C.class);

		final B b = new B();
		final C c = new C();
		b.c = Ref.create(c);

		ofy().save().entities(b, c).now();

		ofy().clear();
		B b1 = ofy().load().entity(b).get();

		A a = new A();
		a.b = Ref.create(b1);

		ofy().save().entity(a).now();

		A a2 = ofy().load().group(BGroup.class).entity(a).get();
		B b2 = a2.b.get();
		assert b2.c.getValue() == null;

		A a3 = ofy().load().group(BGroup.class, CGroup.class).entity(a).get();
		B b3 = a3.b.get();
		C c3 = b3.c.get();	// this used to fail with Ref<?> value has not been initialized

		assert c3 != null;
	}
}
