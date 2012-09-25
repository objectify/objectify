package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cache.CacheControl;

/** 
 * Implements CacheControl for Objectify 
 */
public class CacheControlImpl implements CacheControl
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	public CacheControlImpl(ObjectifyFactory fact)
	{
		this.fact = fact;
	}
	
	/** */
	@Override
	public Integer getExpirySeconds(Key key)
	{
		return fact.getMetadata(key).getCacheExpirySeconds();
	}
}