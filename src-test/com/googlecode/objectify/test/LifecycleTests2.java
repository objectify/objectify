package com.googlecode.objectify.test;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.Test;

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
		@Load @Parent Org org;
		@Id Long id;
	}
	
	/** */
	@Entity
	public static class Product {
		@Id Long id;
		@Load Event event;
		
		@OnLoad void onLoad() {
			assert event.org.foo.equals("fooValue");
		}
	}

	/** */
	@Entity
	public static class Ticket {
		@Id Long id;
		@Load Product product;
	}

	/** */
	@Entity
	public static class TicketGroup {
		@Id Long id;
		@Load List<Ticket> tickets = new ArrayList<Ticket>();
	}

	/**
	 * More complicated test of a more complicated structure 
	 */
	@Test
	public void testCrazyComplicated() throws Exception {
		fact.register(Org.class);
		fact.register(Event.class);
		fact.register(Product.class);
		fact.register(Ticket.class);
		fact.register(TicketGroup.class);
		
		TestObjectify ofy = fact.begin();

		Org org = new Org();
		org.foo = "fooValue";
		ofy.put(org);
		
		Event event = new Event();
		event.org = org;
		ofy.put(event);

		Product prod = new Product();
		prod.event = event;
		ofy.put(prod);
		
		Ticket ticket = new Ticket();
		ticket.product = prod;
		ofy.put(ticket);
		
		TicketGroup group = new TicketGroup();
		group.tickets.add(ticket);
		ofy.put(group);
		
		ofy.clear();
		//ofy.load().entity(group).get();
		
		TicketGroup fetched = ofy.load().type(TicketGroup.class).first().get();
		assert fetched.tickets.get(0).product.event.org.foo.equals("fooValue");
	}
}
