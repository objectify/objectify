package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
	protected Set<String> loadGroups;
	
	/** */
	LoaderImpl(ObjectifyImpl ofy) {
		super(null);
		this.ofy = ofy;
		this.loadGroups = Collections.<String>emptySet();
	}

	/**
	 * Takes ownership of the fetch groups set.
	 */
	LoaderImpl(ObjectifyImpl ofy, Set<String> loadGroups) {
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
	 * @see com.googlecode.objectify.cmd.Loading#group(java.lang.String[])
	 */
	@Override
	public Loader group(String... groupName) {
		Set<String> next = new HashSet<String>(Arrays.asList(groupName));
		next.addAll(this.loadGroups);
		return new LoaderImpl(ofy, next);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Find#type(java.lang.Class)
	 */
	@Override
	public <E> LoadType<E> type(Class<E> type) {
		return new LoadTypeImpl<E>(this, type);
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
		LoadEngine batch = createLoadEngine();
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
	public Set<String> getLoadGroups() {
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
	 * @param groups is the set of load groups that are active
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
