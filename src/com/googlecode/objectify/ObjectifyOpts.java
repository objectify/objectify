package com.googlecode.objectify;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;

/**
 * <p>The options available when creating an Objectify instance.</p>
 * 
 * <p>The default options are:</p>
 * 
 * <ul>
 * <li>Do NOT begin a transaction.</li>
 * <li>Do NOT use a session cache.</li>
 * <li>DO use a global cache.</li>
 * <li>Use STRONG consistency.</li>
 * <li>Apply no deadline to calls.</li>
 * </ul>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectifyOpts
{
	boolean beginTransaction;
	boolean sessionCache = false;
	boolean globalCache = true;
	Consistency consistency = Consistency.STRONG;
	Double deadline;
	
	/** Gets the current value of beginTransaction */
	public boolean getBeginTransaction() { return this.beginTransaction; }
	
	/**
	 * Sets whether or not the Objectify instance will start a transaction.  If
	 * true, the instance will hold a transaction that must be rolled back or
	 * committed.
	 */
	public ObjectifyOpts setBeginTransaction(boolean value)
	{
		this.beginTransaction = value;
		return this;
	}
	
	/** Gets whether or not the Objectify instance will maintain a session cache */
	public boolean getSessionCache() { return this.sessionCache; }
	
	/**
	 * Sets whether or not the Objectify instance will maintain a session cache.
	 * If true, all entities fetched from the datastore (or the 2nd level memcache)
	 * will be stored as-is in a hashmap within the Objectify instance.  Repeated
	 * get()s or queries for the same entity will return the same object.
	 */
	public ObjectifyOpts setSessionCache(boolean value)
	{
		this.sessionCache = value;
		return this;
	}
	
	/** Gets whether or not the Objectify instance will use a 2nd-level memcache */
	public boolean getGlobalCache() { return this.globalCache; }
	
	/**
	 * Sets whether or not the Objectify instance will use a 2nd-level memcache.
	 * If true, Objectify will obey the @Cached annotation on entity classes,
	 * saving entity data to the GAE memcache service.  Fetches from the datastore
	 * for @Cached entities will look in the memcache service first.  This cache
	 * is shared across all versions of your application across the entire GAE
	 * cluster.
	 */
	public ObjectifyOpts setGlobalCache(boolean value)
	{
		this.globalCache = value;
		return this;
	}
	
	/** Gets the initial consistency setting for the Objectify instance */
	public Consistency getConsistency() { return this.consistency; }
	
	/**
	 * Sets the initial consistency value for the Objectify instance.  See the 
	 * <a href="http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/ReadPolicy.Consistency.html">Appengine Docs</a>
	 * for an explanation of Consistency. 
	 */
	public ObjectifyOpts setConsistency(Consistency value)
	{
		if (value == null)
			throw new IllegalArgumentException("Consistency cannot be null");
		
		this.consistency = value;
		return this;
	}

	/** Gets the deadline for datastore calls, in seconds */
	public Double getDeadline() { return this.deadline; }
	
	/**
	 * Sets a limit, in seconds, for datastore calls.  If datastore calls take longer
	 * than this amount, an exception will be thrown.
	 * 
	 * @param value can be null to indicate no deadline (other than the standard whole
	 * request deadline of 30s).
	 */
	public ObjectifyOpts setDeadline(Double value)
	{
		this.deadline = value;
		return this;
	}
}