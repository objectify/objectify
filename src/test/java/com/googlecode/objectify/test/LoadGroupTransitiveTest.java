package com.googlecode.objectify.test;

import lombok.Data;
import org.junit.jupiter.api.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
class LoadGroupTransitiveTest extends TestBase {

	@Entity
	@Data
	private static class A {
		@Id
		long id = 1;

		@Index String foo = "foo";

		@Load(BGroup.class)
		Ref<B> b;
	}

	@Entity
	@Data
	private static class B {
		@Id
		long id = 1;

		@Load(CGroup.class)
		Ref<C> c;
	}

	@Entity
	@Data
	private static class C {
		@Id
		long id = 1;
	}

	private static class BGroup {}
	private static class CGroup {}

	@Test
	void testTransitiveLoad() {
		factory().register(A.class);
		factory().register(B.class);
		factory().register(C.class);

		final B b = new B();
		final C c = new C();
		b.c = Ref.create(c);

		ofy().save().entities(b, c).now();

		ofy().clear();
		final B b1 = ofy().load().entity(b).now();

		final A a = new A();
		a.b = Ref.create(b1);	// the problem was that this created a ResultNow<?>-based Ref which was then skipped for upgrades

		ofy().save().entity(a).now();

		final A a2 = ofy().load().group(BGroup.class, CGroup.class).entity(a).now();
		final B b2 = a2.b.get();
		final C c2 = b2.c.get();	// this used to fail with Ref<?> value has not been initialized

		assertThat(c2).isNotNull();
	}

	@Test
	void testTransitiveLoadStepwise() {
		factory().register(A.class);
		factory().register(B.class);
		factory().register(C.class);

		final B b = new B();
		final C c = new C();
		b.c = Ref.create(c);

		ofy().save().entities(b, c).now();

		ofy().clear();
		final B b1 = ofy().load().entity(b).now();

		final A a = new A();
		a.b = Ref.create(b1);

		ofy().save().entity(a).now();

		final A a2 = ofy().load().group(BGroup.class).entity(a).now();
		final B b2 = a2.b.get();
		assertThat(b2.c.getValue()).isNull();

		final A a3 = ofy().load().group(BGroup.class, CGroup.class).entity(a).now();
		final B b3 = a3.b.get();
		final C c3 = b3.c.get();	// this used to fail with Ref<?> value has not been initialized

		assertThat(c3).isNotNull();
	}
}
