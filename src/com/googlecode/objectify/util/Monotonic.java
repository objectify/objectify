/*
 * $Id$
 */

package com.googlecode.objectify.util;

import java.lang.reflect.Field;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Provides a method for generating contiguous, monotonically increasing values.</p>
 * 
 * <p>The datastore will automatically generate unique ids for your entities if you
 * use a Long @Id value and create an object with a null id.  Unfortunately this id
 * is not guaranteed to increase monotonically, so you cannot use it for time-ordering.
 * You also cannot use datestamps for time-ordering because clock skew among GAE instances
 * is not guaranteed to fall within any particular bound; they could, in theory, be
 * minutes off.</p>
 * 
 * <p>This class uses the memcache service to efficiently generate a monotonically
 * increasing number which *can* be used for time-ordering.  This can be used, for example,
 * to create revision ids.  It can also be used to generate the change numbers needed to keep
 * a slave database in sync.</p>
 * 
 * <p>The data must be stored on a field of an entity.  The field MUST be indexed.</p>
 * 
 * @author Jeff Schnitzer
 */
public class Monotonic
{
	/**
	 * Get the next unique, monotonically increasing, contiguous value for a field
	 * on an entity class.  The field MUST be indexed or this method will always
	 * return 1.
	 */
	public static long next(Objectify ofy, Class<?> entityClass, String fieldName)
	{
		String cacheKey = "monotonic-" + entityClass.getName() + "." + fieldName;
		
		MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
		Long nextValue = memcache.increment(cacheKey, 1);
		if (nextValue == null)
		{
			long lastValue = getMax(ofy, entityClass, fieldName);
			nextValue = memcache.increment(cacheKey, 1, lastValue);
		}
		
		if (nextValue == null)
			throw new IllegalStateException("Memcache service currently not operating");
		else
			return nextValue;
	}
	
	/**
	 * Query for the max value of a field for all entities, or return 1 if there are no
	 * entities.  The field must be a numeric type and it MUST be indexed.
	 */
	private static long getMax(Objectify ofy, Class<?> entityClass, String fieldName)
	{
		Object thing = ofy.query(entityClass).order("-" + fieldName).get();
		
		if (thing != null)
		{
			try
			{
				Field f = TypeUtils.getDeclaredField(entityClass, fieldName);
				f.setAccessible(true);
				Number n = (Number)f.get(thing);
				if (n == null)
					return 0;
				else
					return n.longValue();
			}
			catch (Exception e) { throw new IllegalStateException(e); }
		}
		else
			return 0;
	}

}