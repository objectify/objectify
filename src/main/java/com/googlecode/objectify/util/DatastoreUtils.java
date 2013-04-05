package com.googlecode.objectify.util;

import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.googlecode.objectify.Key;
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
	 * Construct a Key<?> from a Long or String id
	 * @param id must be either Long or String
	 */
	public static <T> Key<T> createKey(Key<?> parent, Class<T> kind, Object id) {
		Key<T> key = null;
		if (id instanceof String) {
			key = Key.create(parent, kind, (String)id);
		}
		else if (id instanceof Long) {
			key = Key.create(parent, kind, (Long)id);
		}
		else if (id instanceof com.google.appengine.api.datastore.Key) {
			key = Key.create((com.google.appengine.api.datastore.Key) id);
		}
		else if (id instanceof Key) {
			key = (Key<T>) id;
		}
		else {
			throw new IllegalArgumentException("id '" + id + "' must be String, Long, Key<?> or com.google.appengine.api.datastore.Key");
		}
		
		if (parent != null && key != null && key.getParent() != null && !parent.equals(key.getParent())) {
			throw new IllegalArgumentException("Parent/Id mismatch.  Attempt to place id: " + key + " under parent: " + parent);
		}
		
		return key;
	}

	/**
	 * Construct a Key from a Long, String, com.google.appengine.api.datastore.Key or Key
	 * @param id must be either Long or String
	 */
	public static com.google.appengine.api.datastore.Key createKey(com.google.appengine.api.datastore.Key parent, String kind, Object id) {
		com.google.appengine.api.datastore.Key key = null;
		if (id instanceof String) {
			key = KeyFactory.createKey(parent, kind, (String)id);
		}
		else if (id instanceof Long) {
			key = KeyFactory.createKey(parent, kind, (Long)id);
		}
		else if (id instanceof com.google.appengine.api.datastore.Key) {
			key = (com.google.appengine.api.datastore.Key) id;
		}
		else if (id instanceof Key) {
			key = ((Key<?>) id).getRaw();
		}
		else {
			throw new IllegalArgumentException("id '" + id + "' must be String, Long, Key<?> or com.google.appengine.api.datastore.Key");
		}
		
		if (parent != null && key != null && key.getParent() != null && !parent.equals(key.getParent())) {
			throw new IllegalArgumentException("Parent/Id mismatch.  Attempt to place id: " + key + " under parent: " + parent);
		}
		
		return key;
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

	/**
	 * Make a list of Key<?>s
	 * @param ids must contain either Long or String
	 */
	public static <T> List<Key<T>> createKeys(Key<?> parent, Class<T> kind, Iterable<?> ids) {
		List<Key<T>> keys = new ArrayList<Key<T>>();
		
		for (Object id: ids)
			keys.add(createKey(parent, kind, id));
		
		return keys;
	}

	/**
	 * Gets the String or Long id from the key, or null if incomplete
	 */
	@SuppressWarnings("unchecked")
	public static <S> S getId(com.google.appengine.api.datastore.Key key) {
		if (!key.isComplete())
			return null;
		else if (key.getName() != null)
			return (S)key.getName();
		else
			return (S)(Long)key.getId();
	}

	/**
	 * Gets the String or Long id from the key, or null if incomplete
	 */
	public static <S> S getId(Key<?> key) {
			return getId(key.getRaw());
	}
}

