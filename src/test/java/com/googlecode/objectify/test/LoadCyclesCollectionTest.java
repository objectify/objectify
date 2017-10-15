package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
class LoadCyclesCollectionTest extends TestBase {

	@Entity
	@Data
	private static class A {
		@Id
		private long id = 1;

		@Load
		private List<Ref<B>> b = new ArrayList<>();
	}

	@Entity
	@Data
	private static class B {
		@Id
		private long id = 2;

		@Load
		private Ref<A> a;
	}

	@Test
	void loadCycles() {
		factory().register(A.class);
		factory().register(B.class);

		final A a = new A();
		final B b = new B();
		b.a = Ref.create(a);
		a.b.add(Ref.create(b));

		ofy().save().entities(a, b).now();

		ofy().clear();
		final A a1 = ofy().load().entity(a).now();
		assertThat(a1.b.get(0).get().a).isNotNull();
	}
}
