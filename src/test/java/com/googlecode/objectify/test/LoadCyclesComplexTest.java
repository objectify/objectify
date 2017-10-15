package com.googlecode.objectify.test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.factory;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
class LoadCyclesComplexTest extends TestBase {

	@Entity
	@Data
	private static class A {
		@Id
		private long id = 1;
		private AEm aem = new AEm();
	}

	@Data
	private static class AEm {
		@Load
		private List<Ref<B>> bs = new ArrayList<>();
	}

	@Entity
	@Data
	private static class B {
		@Parent
		@Load
		private Ref<A> a;

		@Id
		private long id = 2;

		@Load
		private List<Ref<C>> cs = new ArrayList<>();
	}

	@Entity
	@Data
	private static class C {
		@Parent
		@Load
		private Ref<A> a;

		@Id
		private long id = 3;
	}

	@Test
	void loadCycles() {
		factory().register(A.class);
		factory().register(B.class);
		factory().register(C.class);

		final A a = new A();
		final B b = new B();
		final C c = new C();

		c.a = Ref.create(a);
		b.a = Ref.create(a);
		b.cs.add(Ref.create(c));
		a.aem.bs.add(Ref.create(b));

		ofy().save().entities(a, b, c).now();
		ofy().clear();

		final A a1 = ofy().load().entity(a).now();
		assertThat(a1.aem.bs.get(0).get().cs.get(0).get().a).isNotNull();
	}
}
