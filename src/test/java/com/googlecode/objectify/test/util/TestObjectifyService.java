/*
 */

package com.googlecode.objectify.test.util;

import com.googlecode.objectify.ObjectifyService;

/**
 * Gives us our custom version rather than the standard Objectify one. 
 * 
 * @author Jeff Schnitzer
 */
public class TestObjectifyService
{
	static {
		ObjectifyService.setFactory(new TestObjectifyFactory());
	}
	
	/**
	 * @return our extension to Objectify
	 */
	public static TestObjectify ofy() {
		return (TestObjectify)ObjectifyService.ofy();
	}

	/**
	 * @return our extension to ObjectifyFactory
	 */
	public static TestObjectifyFactory factory() {
		return (TestObjectifyFactory)ObjectifyService.factory();
	}
}