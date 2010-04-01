package com.googlecode.objectify;



/**
 * <p>This is a simple container for a single static instance of ObjectifyFactory.
 * You can choose to use this class or build your own equivalent.  You may prefer
 * to use a dependency injection system like Weld or Guice directly with an
 * ObjectifyFactory and eschew this class entirely.</p>
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
	
	/** @see ObjectifyFactory#beginTransaction() */
	public static Objectify beginTransaction() { return factory().beginTransaction(); }
	
	/** @see ObjectifyFactory#begin(ObjectifyOpts) */
	public static Objectify begin(ObjectifyOpts opts) { return factory().begin(opts); }
	
	/** @see ObjectifyFactory#register(Class) */
	public static void register(Class<?> clazz) { factory().register(clazz); }
}