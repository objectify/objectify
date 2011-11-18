package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.LoadCmd;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.util.DatastoreUtils;


/**
 * Implementation of the Find interface.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadCmdImpl extends Queryable<Object> implements LoadCmd
{
	/** */
	LoadCmdImpl(ObjectifyImpl ofy) {
		super(ofy, Collections.<String>emptySet());
	}

	/**
	 * Takes ownership of the fetch groups set.
	 */
	LoadCmdImpl(ObjectifyImpl ofy, Set<String> fetchGroups) {
		super(ofy, fetchGroups);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryDefinition#createQuery()
	 */
	@Override
	QueryImpl<Object> createQuery() {
		return new QueryImpl<Object>(ofy, fetchGroups);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loading#group(java.lang.String[])
	 */
	@Override
	public LoadCmd group(String... groupName) {
		Set<String> next = new HashSet<String>(Arrays.asList(groupName));
		next.addAll(this.fetchGroups);
		return new LoadCmdImpl(ofy, next);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#type(java.lang.Class)
	 */
	@Override
	public <E> LoadType<E> type(Class<E> type) {
		return new LoadTypeImpl<E>(ofy, fetchGroups, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#ref(com.googlecode.objectify.Ref)
	 */
	@Override
	public void ref(Ref<?> ref) {
		refs(Collections.<Ref<?>>singleton(ref));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#refs(com.googlecode.objectify.Ref<?>[])
	 */
	@Override
	public void refs(Ref<?>... refs) {
		refs(Arrays.asList(refs));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#refs(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void refs(Iterable<? extends Ref<?>> refs) {
		List<com.google.appengine.api.datastore.Key> keys = DatastoreUtils.getRawKeys(refs);
		
		final Map<Key<Object>, Object> fetched = ofy.createGetEngine(fetchGroups).get(keys);
		
		for (final Ref<?> ref: refs) {
			Result<?> result = new Result<Object>() {
				@Override
				public Object now() {
					return fetched.get(ref.getKey());
				}
			};
			ref.setResult((Result)result);
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entity(com.googlecode.objectify.Key)
	 */
	@Override
	public <K> Ref<K> entity(Key<K> key) {
		Ref<K> ref = Ref.create(key);
		ref(ref);
		return ref;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entity(com.google.appengine.api.datastore.Key)
	 */
	@Override
	public <K> Ref<K> entity(com.google.appengine.api.datastore.Key rawKey) {
		return entity(Key.<K>create(rawKey));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entity(java.lang.Object)
	 */
	@Override
	public <E, K extends E> Ref<K> entity(E entity) {
		return entity(ofy.getFactory().<K>getKey(entity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entities(java.lang.Object[])
	 */
	@Override
	public <E, K extends E> Map<Key<K>, E> entities(Object... values) {
		return entities(Arrays.asList(values));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entities(java.lang.Iterable)
	 */
	@Override
	public <E, K extends E> Map<Key<K>, E> entities(Iterable<?> values) {
		List<com.google.appengine.api.datastore.Key> raw = ofy.getFactory().getRawKeys(values);
		return ofy.createGetEngine(fetchGroups).get(raw);
	}
}
