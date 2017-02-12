package com.googlecode.objectify.cache;

import com.google.appengine.api.memcache.MemcacheService;

/**
 * This interface is to provide {@link MemcacheService}. The default implementation simply delegates
 * to {@link com.google.appengine.api.memcache.MemcacheServiceFactory}.
 *
 * @author Yan Zhao <zhaoyan1117@gmail.com>
 */
public interface MemcacheServiceFactory {
	MemcacheServiceFactory DEFAULT = new DefaultMemcacheServiceFactory();

	MemcacheService getMemcacheService(String namespace);

	class DefaultMemcacheServiceFactory implements MemcacheServiceFactory {
		@Override
		public MemcacheService getMemcacheService(String namespace) {
			return com.google.appengine.api.memcache.MemcacheServiceFactory.getMemcacheService(namespace);
		}
	}
}
