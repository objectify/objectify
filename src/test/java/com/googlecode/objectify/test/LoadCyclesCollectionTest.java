package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This test was contributed: https://code.google.com/p/objectify-appengine/issues/detail?id=144
 */
public class LoadCyclesCollectionTest extends TestBase {

	@Entity
	public static class A {
		@Id
		public long id = 1;

		@Load
		public List<Ref<B>> b = new ArrayList<Ref<B>>();
	}

	@Entity
	public static class B {
		@Id
		public long id = 2;

		@Load
		public Ref<A> a;
	}

	@Test
	public void loadCycles() {
		this.fact.register(A.class);
		this.fact.register(B.class);

		A a = new A();
		B b = new B();
		b.a = Ref.create(a);
		a.b.add(Ref.create(b));

		ofy().save().entities(a, b).now();

		ofy().clear();
		A a1 = ofy().load().entity(a).get();
		assert a1.b.get(0).get().a != null;
	}
}
