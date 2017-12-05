package com.googlecode.objectify.cache;

import com.google.cloud.datastore.Key;


/**
 * Interface for tracking hit rates of the entity memcache. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface MemcacheStats
{
	public void recordHit(Key key);
	public void recordMiss(Key key);
}


