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
	private ObjectifyFactory fact;

	/** */
	public CacheControlImpl(ObjectifyFactory fact)
	{
		this.fact = fact;
	}

	/** */
	@Override
	public Integer getExpirySeconds(Key key)
	{
		EntityMetadata<?> meta = fact.getMetadata(key.getKind());
		return meta == null ? null : meta.getCacheExpirySeconds();
	}
}
