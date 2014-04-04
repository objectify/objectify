/*
 */

package com.googlecode.objectify;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Holder of the master ObjectifyFactory and provider of the current thread-local Objectify instance.
 * Call {@code ofy()} at any point to get the current Objectify with the correct transaction context.
 * 
 * @author Jeff Schnitzer
 */
public class ObjectifyService
{
	/** */
	private static ObjectifyFactory factory = new ObjectifyFactory();

	/** */
	public static void setFactory(ObjectifyFactory fact) {
		factory = fact;
	}

	/**
	 * Thread local stack of Objectify instances corresponding to transaction depth
	 */
	private static final ThreadLocal<Deque<Objectify>> STACK = new ThreadLocal<Deque<Objectify>>() {
		@Override
		protected Deque<Objectify> initialValue() {
			return new ArrayDeque<Objectify>();
		}
	};

	/**
	 * The method to call at any time to get the current Objectify, which may change depending on txn context
	 */
	public static Objectify ofy() {
		Deque<Objectify> stack = STACK.get();
		if (stack.isEmpty())
			stack.add(factory.begin());

		return stack.getLast();
	}

	/**
	 * @return the current factory
	 */
	public static ObjectifyFactory factory() {
		return factory;
	}

	/**
	 * A shortcut for {@code ObjectifyFactory.register()}
	 *  
	 * @see ObjectifyFactory#register(Class) 
	 */
	public static void register(Class<?> clazz) {
		factory().register(clazz); 
	}
	
	/** Pushes new context onto stack when a transaction starts */
	public static void push(Objectify ofy) {
		STACK.get().add(ofy);
	}

	/** Pops context off of stack after a transaction completes */
	public static void pop() {
		STACK.get().removeLast();
	}

	/** Clear the stack of any leftover Objectify instances */
	public static void reset() {
		STACK.get().clear();
	}
}