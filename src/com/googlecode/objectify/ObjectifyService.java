package com.googlecode.objectify;


/**
 * <p>This is a simple container for a single static instance of ObjectifyFactory.</p>
 * 
 * <p>It is generally wise to extend this class and register your entities.  See the
 * <a href="http://code.google.com/p/objectify-appengine/wiki/BestPractices">BestPractices</a>.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyService
{
	/** Singleton instance */
	protected static ObjectifyFactory factory = new ObjectifyFactory();
	
	/** Call this to get the instance */
	public static ObjectifyFactory fact() { return factory; }
}