package com.googlecode.objectify;



/**
 * <p>This is a simple container for a single static instance of ObjectifyFactory.
 * You can choose to use this class or build your own equivalent - look at the source
 * code, there are only four lines.  If you use a dependency injection system like Weld
 * or Guice, you do not need this class at all - simply inject an ObjectifyFactory.</p>
 * 
 * <p>For further advice, see the
 * <a href="http://code.google.com/p/objectify-appengine/wiki/BestPractices">BestPractices</a>.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyService
{
	/** Singleton instance */
	protected static ObjectifyFactory factory = new ObjectifyFactory();
	
	/** Call this to get the instance */
	public static ObjectifyFactory factory() { return factory; }

	//
	// All static methods simply pass-through to the singleton factory
	//

	/** @see ObjectifyFactory#begin() */
	public static Objectify begin() { return factory().begin(); }
	
	/** @see ObjectifyFactory#register(Class) */
	public static void register(Class<?> clazz) { factory().register(clazz); }
}