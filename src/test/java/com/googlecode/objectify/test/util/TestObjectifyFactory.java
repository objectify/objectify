package com.googlecode.objectify.test.util;

import com.google.appengine.api.memcache.IMemcacheServiceFactory;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cache.EntityMemcache;
import com.googlecode.objectify.impl.CacheControlImpl;

/**
 * Primarily exists to enable the TestObjectify
 */
public class TestObjectifyFactory extends ObjectifyFactory
{
	@Override
	public Objectify begin() {
		return new TestObjectify(this)

			// This can be used to enable/disable the memory cache globally.
			.cache(true);
	}

	/** Only used for one test */
	public void setMemcacheFactory(final IMemcacheServiceFactory factory) {
		this.entityMemcache = new EntityMemcache(MEMCACHE_NAMESPACE, new CacheControlImpl(this), this.memcacheStats, factory);
	}
}
