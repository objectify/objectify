package com.googlecode.objectify;


/**
 * <p>This is a simple container for a single static instance of ObjectifyFactory.</p>
 * 
 * <p>You can choose to use this class or build your own equivalent.  You may prefer
 * to use a Dependency Injection system like Weld or Guice.</p>
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
}