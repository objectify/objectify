package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.impl.engine.QueryEngine;
import com.googlecode.objectify.util.ResultProxy;


/**
 * Implementation of the Loader interface.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoaderImpl extends Queryable<Object> implements Loader
{
	/** */
	protected Loader wrapper = this;
	
	/** */
	protected ObjectifyImpl ofy;
	
	/** */
	protected Set<Class<?>> loadGroups;
	
	/** */
	LoaderImpl(ObjectifyImpl ofy) {
		super(null);
		this.ofy = ofy;
		this.loadGroups = Collections.<Class<?>>emptySet();
	}

	/**
	 * Takes ownership of the fetch groups set.
	 */
	LoaderImpl(ObjectifyImpl ofy, Set<Class<?>> loadGroups) {
		super(null);
		this.ofy = ofy;
		this.loadGroups = Collections.unmodifiableSet(loadGroups);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryDefinition#createQuery()
	 */
	@Override
	QueryImpl<Object> createQuery() {
		return new QueryImpl<Object>(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#group(java.lang.Class<?>[])
	 */
	@Override
	public Loader group(Class<?>... groups) {
		Set<Class<?>> next = new HashSet<Class<?>>(Arrays.asList(groups));
		next.addAll(this.loadGroups);
		return new LoaderImpl(ofy, next);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#type(java.lang.Class)
	 */
	@Override
	public <E> LoadType<E> type(Class<E> type) {
		return new LoadTypeImpl<E>(this, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#ref(com.googlecode.objectify.Ref)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <K> Ref<K> ref(Ref<K> ref) {
		refs(ref);
		return ref;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#refs(com.googlecode.objectify.Ref<?>[])
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> refs(Ref<E>... refs) {
		return refs(Arrays.asList(refs));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#refs(java.lang.Iterable)
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> refs(final Iterable<Ref<E>> refs) {
		LoadEngine batch = createLoadEngine();
		for (Ref<?> ref: refs)
			batch.loadRef(ref);
		
		batch.execute();

		// Now asynchronously translate into a normal-looking map
		@SuppressWarnings("unchecked")
		Map<Key<K>, E> map = ResultProxy.create(Map.class, new Result<Map<Key<K>, E>>() {
			@Override
			public Map<Key<K>, E> now() {
				Map<Key<K>, E> result = new LinkedHashMap<Key<K>, E>();
				for (Ref<E> ref: refs)
					if (ref.get() != null)
						result.put((Key<K>)ref.key(), (E)ref.get());

				return result;
			}
		});
		
		return map;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(com.googlecode.objectify.Key)
	 */
	@Override
	public <K> Ref<K> key(Key<K> key) {
		Ref<K> ref = Ref.create(key);
		return ref(ref);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(java.lang.Object)
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
			return ref((Ref<K>)key);
		} else {
			return (Ref<K>)key(ofy.getFactory().getKey(key));
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(com.googlecode.objectify.Key<E>[])
	 */
	@Override
	public <K, E extends K> Map<Key<K>, E> keys(Key<E>... keys) {
		return this.<K, E>keys(Arrays.asList(keys));
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
		return this.<K, E>entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(java.lang.Iterable)
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
	@SuppressWarnings("unchecked")
	public <K, E extends K> Map<Key<K>, E> values(Iterable<?> values) {
		final List<Ref<E>> refs = new ArrayList<Ref<E>>();
		
		for (Object keyish: values) {
			if (keyish instanceof Ref) {
				refs.add((Ref<E>)keyish);
			} else {
				Key<E> key = ofy.getFactory().getKey(keyish);
				refs.add(Ref.create(key));
			}
		}
		
		return this.refs(refs);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#getObjectify()
	 */
	@Override
	public Objectify getObjectify() {
		return ofy.getWrapper();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#getLoadGroups()
	 */
	@Override
	public Set<Class<?>> getLoadGroups() {
		// This is unmodifiable
		return loadGroups;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#setWrapper(com.googlecode.objectify.cmd.Loader)
	 */
	@Override
	public void setWrapper(Loader loader) {
		this.wrapper = loader;
	}
	
	/** */
	public ObjectifyImpl getObjectifyImpl() {
		return this.ofy;
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for load commands
	 */
	public LoadEngine createLoadEngine() {
		return new LoadEngine(this);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for queries
	 */
	public QueryEngine createQueryEngine() {
		return new QueryEngine(this);
	}

}
