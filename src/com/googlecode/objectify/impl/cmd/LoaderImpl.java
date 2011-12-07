package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.util.ResultProxy;


/**
 * Implementation of the Find interface.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoaderImpl extends Queryable<Object> implements Loader
{
	/** */
	LoaderImpl(ObjectifyImpl ofy) {
		super(ofy, Collections.<String>emptySet());
	}

	/**
	 * Takes ownership of the fetch groups set.
	 */
	LoaderImpl(ObjectifyImpl ofy, Set<String> fetchGroups) {
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
	public Loader group(String... groupName) {
		Set<String> next = new HashSet<String>(Arrays.asList(groupName));
		next.addAll(this.fetchGroups);
		return new LoaderImpl(ofy, next);
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
		refs(ref);
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
	public void refs(Iterable<? extends Ref<?>> refs) {
		LoadEngine batch = ofy.createLoadEngine(fetchGroups);
		for (Ref<?> ref: refs)
			batch.loadRef(ref);
		
		batch.execute();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entity(com.googlecode.objectify.Key)
	 */
	@Override
	public <K> Ref<K> key(Key<K> key) {
		Ref<K> ref = Ref.create(key);
		ref(ref);
		return ref;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entity(java.lang.Object)
	 */
	@Override
	public <K, E extends K> Ref<K> entity(E entity) {
		return key(ofy.getFactory().<K>getKey(entity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#value(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <K> Ref<K> value(Object key) {
		if (key instanceof Ref) {
			ref((Ref<K>)key);
			return (Ref<K>)key;
		} else {
			return (Ref<K>)key(ofy.getFactory().getKey(key));
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(com.googlecode.objectify.Key<E>[])
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> keys(Key<E>... keys) {
		return keys(Arrays.asList(keys));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(java.lang.Iterable)
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> keys(Iterable<Key<E>> keys) {
		return values(keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(E[])
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> entities(E... entities) {
		return entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#entities(java.lang.Iterable)
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> entities(Iterable<E> values) {
		return values(values);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Object[])
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> values(Object... values) {
		return values(Arrays.asList(values));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Iterable)
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> values(Iterable<?> values) {
		final Map<Key<?>, Ref<?>> refs = new LinkedHashMap<Key<?>, Ref<?>>();
		
		for (Object keyish: values) {
			if (keyish instanceof Ref) {
				Ref<?> ref = (Ref<?>)keyish;
				refs.put(ref.getKey(), ref);
			} else {
				Key<?> key = ofy.getFactory().getKey(keyish);
				refs.put(key, Ref.create(key));
			}
		}
		
		// Get real results
		this.refs(refs.values());
		
		// Now asynchronously translate into a normal-looking map
		@SuppressWarnings("unchecked")
		Map<Key<K>, E> map = ResultProxy.create(Map.class, new Result<Map<Key<K>, E>>() {
			@Override
			public Map<Key<K>, E> now() {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>();
				for (Ref<?> ref: refs.values())
					if (ref.get() != null)
						result.put((Key<K>)ref.key(), (E)ref.get());

				return result;
			}
		});
		
		return map;
	}
}
