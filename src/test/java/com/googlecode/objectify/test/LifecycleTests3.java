package com.googlecode.objectify.test;

import org.testng.annotations.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;

import static com.googlecode.objectify.test.util.TestObjectifyService.fact;
import static com.googlecode.objectify.test.util.TestObjectifyService.ofy;

/**
 * Trying to narrow down a specific problem.
 */
public class LifecycleTests3 extends TestBase
{
	/** */
	@Entity
	public static class Event {
		@Id Long id;
		String foo;
	}

	/** */
	@Entity
	public static class Product {
		@Load @Parent Ref<Event> event;
		@Id Long id;

		@OnLoad void onLoad() {
			assert event.get().foo.equals("fooValue");
		}
	}

	/**
	 * More complicated test of a more complicated structure
	 */
	@Test
	public void loadingRefInOnLoad() throws Exception {
		fact().register(Event.class);
		fact().register(Product.class);

		Event event = new Event();
		event.foo = "fooValue";
		ofy().put(event);

		Product prod = new Product();
		prod.event = Ref.create(event);
		ofy().put(prod);

		ofy().clear();
		Product fetched = ofy().load().entity(prod).get();
		assert fetched.event.get().foo.equals("fooValue");
	}
}
