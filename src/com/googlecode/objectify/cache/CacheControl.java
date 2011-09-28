package com.googlecode.objectify.cache;

import com.google.appengine.api.datastore.Key;


/**
 * Interface by which expiry times for cache entities is communicated to the cache system.  The cache will
 * call this interface to find out how long to cache entities of a particular kind. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface CacheControl
{
	/**
	 * Get the amount of time that entities of a particular key should be cached, if at all.  This is used
	 * both to write entities/negative results to the cache and also to determine if we should look in the
	 * cache in the first place.
	 * 
	 * @return null means DO NOT CACHE.  0 means "no limit".  Any other value is a # of seconds.
	 */
	public Integer getExpirySeconds(Key key);
}


