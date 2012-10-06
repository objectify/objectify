package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;
import com.googlecode.objectify.test.util.TestObjectify;

/**
 * This is just getting wacky, but there's a bug in there somewhere
 */
public class LifecycleTests2 extends TestBase
{
	/** */
	@Entity
	public static class Org {
		@Id Long id;
		String foo;
	}

	/** */
	@Entity
	public static class Event {
		@Load @Parent Ref<Org> org;
		@Id Long id;
	}

	/** */
	@Entity
	public static class Product {
		@Load @Parent Ref<Event> event;
		@Id Long id;

		@OnLoad void onLoad() {
			assert event.get().org.get().foo.equals("fooValue");
		}
	}

	/**
	 * More complicated test of a more complicated structure
	 */
	@Test
	public void testCrazyComplicated() throws Exception {
		fact.register(Org.class);
		fact.register(Event.class);
		fact.register(Product.class);

		TestObjectify ofy = fact.begin();

		Org org = new Org();
		org.foo = "fooValue";
		ofy.put(org);

		Event event = new Event();
		event.org = Ref.create(org);
		ofy.put(event);

		Product prod = new Product();
		prod.event = Ref.create(event);
		ofy.put(prod);

		ofy.clear();
		Product fetched = ofy.load().entity(prod).get();
		assert fetched.event.get().org.get().foo.equals("fooValue");
	}
}
