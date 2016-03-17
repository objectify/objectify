package com.googlecode.objectify.util;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Projection;
import com.google.appengine.api.datastore.PropertyContainer;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.SaveContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Some static utility methods for interacting with basic datastore objects like keys and queries.
 */
public class DatastoreUtils
{
	private DatastoreUtils() {
	}

	/**
	 * Turn a list of refs into a list of raw keys.
	 */
	public static List<com.google.appengine.api.datastore.Key> getRawKeys(Iterable<? extends Ref<?>> refs) {
		List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<>();
		for (Ref<?> ref: refs)
			rawKeys.add(ref.getKey().getRaw());
		
		return rawKeys;
	}
		
	/**
	 * Make a new Query object that is exactly like the old.  Too bad Query isn't Cloneable. 
	 */
	public static com.google.appengine.api.datastore.Query cloneQuery(com.google.appengine.api.datastore.Query orig) {
		
		com.google.appengine.api.datastore.Query copy = new com.google.appengine.api.datastore.Query(orig.getKind(), orig.getAncestor());
		
		copy.setFilter(orig.getFilter());

		for (SortPredicate sort: orig.getSortPredicates())
			copy.addSort(sort.getPropertyName(), sort.getDirection());

		for (Projection projection: orig.getProjections())
			copy.addProjection(projection);
		
		if (orig.isKeysOnly())
			copy.setKeysOnly();

		copy.setDistinct(orig.getDistinct());
		
		return copy;
	}

	/**
	 * Construct a Key<?> from a Long or String id
	 * @param id must be either Long or String
	 */
	public static <T> Key<T> createKey(Key<?> parent, Class<T> kind, Object id) {
		if (id instanceof String)
			return Key.create(parent, kind, (String)id);
		else if (id instanceof Long)
			return Key.create(parent, kind, (Long)id);
		else
			throw new IllegalArgumentException("id '" + id + "' must be String or Long");
	}

	/**
	 * Construct a Key<?> from a Long or String id
	 * @param id must be either Long or String
	 */
	public static <T> Key<T> createKey(Key<?> parent, String kind, Object id) {
		com.google.appengine.api.datastore.Key key = createKey(parent == null ? null : parent.getRaw(), kind, id);
		return Key.create(key);
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
			throw new IllegalArgumentException("id '" + id + "' must be String or Long");
	}

	/**
	 * Make a list of keys
	 * @param ids must contain either Long or String
	 */
	public static List<com.google.appengine.api.datastore.Key> createKeys(com.google.appengine.api.datastore.Key parent, String kind, Iterable<?> ids) {
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<>();
		
		for (Object id: ids)
			keys.add(createKey(parent, kind, id));
		
		return keys;
	}

	/**
	 * Make a list of Key<?>s
	 * @param ids must contain either Long or String
	 */
	public static <T> List<Key<T>> createKeys(Key<?> parent, Class<T> kind, Iterable<?> ids) {
		List<Key<T>> keys = new ArrayList<>();
		
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

	/**
	 * Unfortunately Entity and EmbeddedEntity do not share a common interface for getting key.
	 */
	public static com.google.appengine.api.datastore.Key getKey(PropertyContainer container) {
		if (container instanceof EmbeddedEntity)
			return ((EmbeddedEntity)container).getKey();
		else if (container instanceof Entity)
			return ((Entity)container).getKey();
		else
			throw new IllegalArgumentException("Unknown type of property container: " + container.getClass());
	}

	/**
	 * Calls setProperty() or setUnindexedProperty() as determined by the index parameter.
	 * Also stuffs any values in the savecontext index.
	 */
	public static void setContainerProperty(PropertyContainer entity, String propertyName, Object value, boolean index, SaveContext ctx, Path propPath) {
		if (index) {
			entity.setProperty(propertyName, value);
			ctx.addIndex(propPath, value);
		} else {
			entity.setUnindexedProperty(propertyName, value);
		}
	}

}

