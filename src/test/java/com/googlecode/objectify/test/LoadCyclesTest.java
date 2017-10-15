package com.googlecode.objectify.test;

import org.junit.jupiter.api.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
class LoadCyclesTest extends TestBase {

	@Entity
	private static class A {
		@Id
		private long id = 1;

		@Load
		private Ref<B> b;
	}

	@Entity
	private static class B {
		@Id
		private long id = 1;

		@Load
		private Ref<A> a;
	}

	@Test
	void loadCycles() {
		factory().register(A.class);
		factory().register(B.class);

		final A a = new A();
		final B b = new B();
		a.b = Ref.create(b);
		b.a = Ref.create(a);

		ofy().save().entities(a, b).now();

		ofy().clear();
		final A a1 = ofy().load().entity(a).now();
		assertThat(a1.b.get().id).isEqualTo(b.id);
	}
}
