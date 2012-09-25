package com.googlecode.objectify.cache;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.ObjectifyFactory;


/**
 * <p>A convenience class with factory methods to create caching versions of DatastoreService
 * and AsyncDatastoreService.  These are just shortcuts for common cases - do not be afraid to
 * use the constructors of CachingDatastoreService or CachingAsyncDatastoreService to create
 * an interface tailored exactly to your caching needs.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CachingDatastoreServiceFactory
{
	private static String defaultMemcacheNamespace = ObjectifyFactory.MEMCACHE_NAMESPACE;
	
	/** 
	 * The default namespace is the one used by Objectify for its cache.  You can reset it.
	 */
	public static void setDefaultMemcacheNamespace(String value) { defaultMemcacheNamespace = value; }
	
	/** The memcache snamespace used by default for most of these factory methods */
	public static String getDefaultMemcacheNamespace() { return defaultMemcacheNamespace; }
	
	/**
	 * Get a caching DatastoreService with no pre-set expiration on cache values.
	 */
	public static DatastoreService getDatastoreService()
	{
		EntityMemcache em = new EntityMemcache(defaultMemcacheNamespace);
		return getDatastoreService(em);
	}
	
	/**
	 * Get a caching DatastoreService with no pre-set expiration on cache values.
	 */
	public static DatastoreService getDatastoreService(DatastoreServiceConfig cfg)
	{
		EntityMemcache em = new EntityMemcache(defaultMemcacheNamespace);
		return getDatastoreService(cfg, em);
	}

	/**
	 * Get a caching AsyncDatastoreService with no pre-set expiration on cache values.
	 */
	public static AsyncDatastoreService getAsyncDatastoreService()
	{
		EntityMemcache em = new EntityMemcache(defaultMemcacheNamespace);
		return getAsyncDatastoreService(em);
	}
	
	/**
	 * Get a caching AsyncDatastoreService with no pre-set expiration on cache values.
	 */
	public static AsyncDatastoreService getAsyncDatastoreService(DatastoreServiceConfig cfg)
	{
		EntityMemcache em = new EntityMemcache(defaultMemcacheNamespace);
		return getAsyncDatastoreService(cfg, em);
	}
	
	/**
	 * Get a caching DatastoreService with a specific expiration on all cached items.
	 */
	public static DatastoreService getDatastoreService(int expirySeconds)
	{
		EntityMemcache em = getCacheControlled(expirySeconds);
		return getDatastoreService(em);
	}
	
	/**
	 * Get a caching DatastoreService with a specific expiration on all cached items.
	 */
	public static DatastoreService getDatastoreService(DatastoreServiceConfig cfg, int expirySeconds)
	{
		EntityMemcache em = getCacheControlled(expirySeconds);
		return getDatastoreService(cfg, em);
	}

	/**
	 * Get a caching AsyncDatastoreService with a specific expiration on all cached items.
	 */
	public static AsyncDatastoreService getAsyncDatastoreService(int expirySeconds)
	{
		EntityMemcache em = getCacheControlled(expirySeconds);
		return getAsyncDatastoreService(em);
	}
	
	/**
	 * Get a caching AsyncDatastoreService with a specific expiration on all cached items.
	 */
	public static AsyncDatastoreService getAsyncDatastoreService(DatastoreServiceConfig cfg, int expirySeconds)
	{
		EntityMemcache em = getCacheControlled(expirySeconds);
		return getAsyncDatastoreService(cfg, em);
	}
	
	/**
	 * Get a caching DatastoreService that uses a particular EntityMemcache configuration. 
	 * Ignores the default memcacheService for this factory; that is set when you construct
	 * your EntityMemcache.
	 */
	public static CachingDatastoreService getDatastoreService(EntityMemcache em)
	{
		CachingAsyncDatastoreService cads = getAsyncDatastoreService(em);
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService();
		return new CachingDatastoreService(ds, cads);
	}

	/**
	 * Get a caching AsyncDatastoreService that uses a particular EntityMemcache configuration.
	 * Ignores the default memcacheService for this factory; that is set when you construct
	 * your EntityMemcache.
	 */
	public static CachingAsyncDatastoreService getAsyncDatastoreService(EntityMemcache em)
	{
		AsyncDatastoreService ads = DatastoreServiceFactory.getAsyncDatastoreService();
		return new CachingAsyncDatastoreService(ads, em);
	}

	/**
	 * Get a caching DatastoreService that uses a particular EntityMemcache configuration.
	 * Ignores the default memcacheService for this factory; that is set when you construct
	 * your EntityMemcache.
	 */
	public static CachingDatastoreService getDatastoreService(DatastoreServiceConfig cfg, EntityMemcache em)
	{
		CachingAsyncDatastoreService cads = getAsyncDatastoreService(cfg, em);
		DatastoreService ds = DatastoreServiceFactory.getDatastoreService(cfg);
		return new CachingDatastoreService(ds, cads);
	}

	/**
	 * Get a caching AsyncDatastoreService that uses a particular EntityMemcache configuration.
	 * Ignores the default memcacheService for this factory; that is set when you construct
	 * your EntityMemcache.
	 */
	public static CachingAsyncDatastoreService getAsyncDatastoreService(DatastoreServiceConfig cfg, EntityMemcache em)
	{
		AsyncDatastoreService ads = DatastoreServiceFactory.getAsyncDatastoreService(cfg);
		return new CachingAsyncDatastoreService(ads, em);
	}

	/**
	 * Get an EntityMemcache that is cache controlled to a specific number of seconds
	 */
	private static EntityMemcache getCacheControlled(final int expirySeconds)
	{
		return new EntityMemcache(defaultMemcacheNamespace, new CacheControl() {
			@Override
			public Integer getExpirySeconds(Key key)
			{
				return expirySeconds;
			}
		});
	}
}