package com.googlecode.objectify.test;

import lombok.Data;
import org.junit.jupiter.api.Test;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.test.util.TestBase;

import static com.google.common.truth.Truth.assertThat;
import static com.googlecode.objectify.ObjectifyService.factory;
import static com.googlecode.objectify.ObjectifyService.ofy;

/**
 * This is just getting wacky, but there's a bug in there somewhere
 */
class LifecycleTests2 extends TestBase
{
	/** */
	@Entity
	@Data
	private static class Org {
		@Id Long id;
		String foo;
	}

	/** */
	@Entity
	@Data
	private static class Event {
		@Load @Parent Ref<Org> org;
		@Id Long id;
	}

	/** */
	@Entity
	@Data
	private static class Product {
		@Load @Parent Ref<Event> event;
		@Id Long id;

		@OnLoad void onLoad() {
			assertThat(event.get().org.get().foo).isEqualTo("fooValue");
		}
	}

	/**
	 * More complicated test of a more complicated structure
	 */
	@Test
	void testCrazyComplicated() throws Exception {
		factory().register(Org.class);
		factory().register(Event.class);
		factory().register(Product.class);

		final Org org = new Org();
		org.foo = "fooValue";
		ofy().save().entity(org).now();

		final Event event = new Event();
		event.org = Ref.create(org);
		ofy().save().entity(event).now();

		final Product prod = new Product();
		prod.event = Ref.create(event);
		ofy().save().entity(prod).now();

		ofy().clear();
		final Product fetched = ofy().load().entity(prod).now();
		assertThat(fetched.event.get().org.get().foo).isEqualTo("fooValue");
	}
}
