package com.googlecode.objectify;

import com.google.appengine.api.datastore.ReadPolicy.Consistency;

/**
 * <p>The options available when creating an Objectify instance.  This is an immutable object;
 * create one by calling {@code ObjectifyOpts.defaults()} and calling methods to create new
 * instances.</p>
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
public class ObjectifyOpts implements Cloneable
{
	boolean sessionCache = false;
	boolean globalCache = true;
	Consistency consistency = Consistency.STRONG;
	Double deadline;
	
	/**
	 * <p>The default options are:</p>
	 * 
	 * <ul>
	 * <li>Do NOT begin a transaction.</li>
	 * <li>Do NOT use a session cache.</li>
	 * <li>DO use a global cache.</li>
	 * <li>Use STRONG consistency.</li>
	 * <li>Apply no deadline to calls.</li>
	 * </ul>
	 */
	public static ObjectifyOpts defaults() {
		return new ObjectifyOpts();
	}
	
	/** Don't make this public */
	private ObjectifyOpts() {}
	
	/** Gets whether or not the Objectify instance will maintain a session cache */
	public boolean getSessionCache() { return this.sessionCache; }
	
	/**
	 * Sets whether or not the Objectify instance will start with a session cache.
	 * If true, all entities fetched from the datastore (or the 2nd level memcache)
	 * will be stored as-is in a hashmap within the Objectify instance.  Repeated
	 * get()s or queries for the same entity will return the same object.
	 */
	public ObjectifyOpts sessionCache(boolean value) {
		ObjectifyOpts clone = this.clone();
		clone.sessionCache = value;
		return clone;
	}
	
	/** Gets whether or not the Objectify instance will use a 2nd-level memcache */
	public boolean getGlobalCache() { return this.globalCache; }
	
	/**
	 * Sets whether or not the Objectify instance will begin using a 2nd-level memcache.
	 * If true, Objectify will obey the @Cache annotation on entity classes,
	 * saving entity data to the GAE memcache service.  Fetches from the datastore
	 * for @Cache entities will look in the memcache service first.  This cache
	 * is shared across all versions of your application across the entire GAE
	 * cluster.
	 */
	public ObjectifyOpts globalCache(boolean value) {
		ObjectifyOpts clone = this.clone();
		clone.globalCache = value;
		return clone;
	}
	
	/** Gets the initial consistency setting for the Objectify instance */
	public Consistency getConsistency() { return this.consistency; }
	
	/**
	 * Sets the initial consistency value for the Objectify instance.  See the 
	 * <a href="http://code.google.com/appengine/docs/java/javadoc/com/google/appengine/api/datastore/ReadPolicy.Consistency.html">Appengine Docs</a>
	 * for an explanation of Consistency. 
	 */
	public ObjectifyOpts consistency(Consistency value) {
		if (value == null)
			throw new IllegalArgumentException("Consistency cannot be null");
		
		ObjectifyOpts clone = this.clone();
		clone.consistency = value;
		return clone;
	}

	/** Gets the deadline for datastore calls, in seconds */
	public Double getDeadline() { return this.deadline; }
	
	/**
	 * Sets a limit, in seconds, for datastore calls.  If datastore calls take longer
	 * than this amount, an exception will be thrown.
	 * 
	 * @param value can be null to indicate no deadline (other than the standard whole
	 * request deadline of 30s/10m).
	 */
	public ObjectifyOpts deadline(Double value) {
		ObjectifyOpts clone = this.clone();
		clone.deadline = value;
		return clone;
	}

	/** Make a copy of this object as-is. */
	@Override
	protected ObjectifyOpts clone()
	{
		try {
			return (ObjectifyOpts)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e);	// impossible
		}
	}
}