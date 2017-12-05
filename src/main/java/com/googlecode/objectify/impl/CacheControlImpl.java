package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cache.CacheControl;

/**
 * Implements CacheControl for Objectify
 */
public class CacheControlImpl implements CacheControl
{
	/** */
	private final ObjectifyFactory fact;

	/** */
	public CacheControlImpl(final ObjectifyFactory fact)
	{
		this.fact = fact;
	}

	/** */
	@Override
	public Integer getExpirySeconds(Key key) {
		final EntityMetadata<?> meta = fact.getMetadata(key.getKind());
		return meta == null ? null : meta.getCacheExpirySeconds();
	}
}