package com.googlecode.objectify.test;

import lombok.Data;
import org.junit.jupiter.api.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
class LoadCyclesDeeperTest extends TestBase {

	@Entity
	@Data
	private static class A {
		@Id
		private long id = 1;

		@Load
		private Ref<B> b;
	}

	@Entity
	@Data
	private static class B {
		@Id
		private long id = 2;

		@Load
		private Ref<C> c;
	}

	@Entity
	@Data
	private static class C {
		@Id
		private long id = 3;

		@Load @Parent
		private Ref<A> a;
	}

	@Test
	void loadCycles() {
		factory().register(A.class);
		factory().register(B.class);
		factory().register(C.class);

		final A a = new A();
		final B b = new B();
		final C c = new C();
		a.b = Ref.create(b);
		c.a = Ref.create(a);	// must do this before creating Ref for b.c
		b.c = Ref.create(c);

		ofy().save().entities(a, b, c).now();

		ofy().clear();
		final A a1 = ofy().load().entity(a).now();
		assertThat(a1.b.get().c.get().a.get()).isNotNull();
	}
}
