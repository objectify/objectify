package com.googlecode.objectify.util;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.googlecode.objectify.Ref;

/**
 * Some static utility methods for interacting with basic datastore objects like keys and queries.
 */
public class DatastoreUtils
{
	/**
	 * Turn a list of refs into a list of raw keys.
	 */
	public static List<com.google.appengine.api.datastore.Key> getRawKeys(Iterable<? extends Ref<?>> refs) {
		List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<com.google.appengine.api.datastore.Key>();
		for (Ref<?> ref: refs)
			rawKeys.add(ref.getKey().getRaw());
		
		return rawKeys;
	}
		
	/**
	 * Make a new Query object that is exactly like the old.  Too bad Query isn't Cloneable. 
	 */
	public static com.google.appengine.api.datastore.Query cloneQuery(com.google.appengine.api.datastore.Query orig) {
		
		com.google.appengine.api.datastore.Query copy = new com.google.appengine.api.datastore.Query(orig.getKind(), orig.getAncestor());
		
		for (FilterPredicate filter: orig.getFilterPredicates())
			copy.addFilter(filter.getPropertyName(), filter.getOperator(), filter.getValue());
		
		for (SortPredicate sort: orig.getSortPredicates())
			copy.addSort(sort.getPropertyName(), sort.getDirection());
		
		if (orig.isKeysOnly())
			copy.setKeysOnly();
		
		return copy;
	}

	/**
	 * Construct a Key from a Long or String id
	 * @param id must be either Long or String
	 */
	public static com.google.appengine.api.datastore.Key createKey(com.google.appengine.api.datastore.Key parent, String kind, Object id) {
		if (id instanceof String)
			return KeyFactory.createKey(parent, kind, (String)id);
		else if (id instanceof Long)
			return KeyFactory.createKey(parent, kind, (Long)id);
		else
			throw new IllegalArgumentException("Id + '" + id + "' must be String or Long");
	}

	/**
	 * Make a list of keys
	 * @param ids must contain either Long or String
	 */
	public static List<com.google.appengine.api.datastore.Key> createKeys(com.google.appengine.api.datastore.Key parent, String kind, Iterable<?> ids) {
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		
		for (Object id: ids)
			keys.add(createKey(parent, kind, id));
		
		return keys;
	}
}

