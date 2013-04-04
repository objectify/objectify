package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
public class LoadCyclesDeeperTest extends TestBase {

	@Entity
	public static class A {
		@Id
		public long id = 1;

		@Load
		public Ref<B> b;
	}

	@Entity
	public static class B {
		@Id
		public long id = 2;

		@Load
		public Ref<C> c;
	}

	@Entity
	public static class C {
		@Id
		public long id = 3;

		@Load @Parent
		public Ref<A> a;
	}

	@Test
	public void loadCycles() {
		this.fact.register(A.class);
		this.fact.register(B.class);
		this.fact.register(C.class);

		A a = new A();
		B b = new B();
		C c = new C();
		a.b = Ref.create(b);
		c.a = Ref.create(a);	// must do this before creating Ref for b.c
		b.c = Ref.create(c);

		ofy().save().entities(a, b, c).now();

		ofy().clear();
		A a1 = ofy().load().entity(a).get();
		assert a1.b.get().c.get().a.get() != null;
	}
}
