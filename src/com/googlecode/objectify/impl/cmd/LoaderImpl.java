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
	public <E> Ref<E> ref(Ref<E> ref) {
		refs(ref);
		return ref;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#refs(com.googlecode.objectify.Ref<?>[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> refs(Ref<? extends E>... refs) {
		return refs(Arrays.asList((Ref<E>[])refs));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#refs(java.lang.Iterable)
	 */
	@Override
	public <E> Map<Key<E>, E> refs(final Iterable<Ref<E>> refs) {
		LoadEngine batch = createLoadEngine();
		for (Ref<?> ref: refs)
			batch.loadRef(ref);
		
		batch.execute();

		// Now asynchronously translate into a normal-looking map
		Map<Key<E>, E> map = ResultProxy.create(Map.class, new Result<Map<Key<E>, E>>() {
			@Override
			public Map<Key<E>, E> now() {
				Map<Key<E>, E> result = new LinkedHashMap<Key<E>, E>();
				for (Ref<E> ref: refs)
					if (ref.get() != null)
						result.put(ref.key(), ref.get());

				return result;
			}
		});
		
		return map;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(com.googlecode.objectify.Key)
	 */
	@Override
	public <E> Ref<E> key(Key<E> key) {
		Ref<E> ref = Ref.create(key);
		return ref(ref);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(java.lang.Object)
	 */
	@Override
	public <E> Ref<E> entity(E entity) {
		return key(ofy.getFactory().<E>getKey(entity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#value(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Ref<E> value(Object key) {
		if (key instanceof Ref) {
			return ref((Ref<E>)key);
		} else {
			return (Ref<E>)key(ofy.getFactory().getKey(key));
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(com.googlecode.objectify.Key<E>[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> keys(Key<? extends E>... keys) {
		return this.keys(Arrays.asList((Key<E>[])keys));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(java.lang.Iterable)
	 */
	@Override
	public <E> Map<Key<E>, E> keys(Iterable<Key<E>> keys) {
		return values(keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(E[])
	 */
	@Override
	public <E> Map<Key<E>, E> entities(E... entities) {
		return this.entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(java.lang.Iterable)
	 */
	@Override
	public <E> Map<Key<E>, E> entities(Iterable<E> values) {
		return values(values);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Object[])
	 */
	@Override
	public <E> Map<Key<E>, E> values(Object... values) {
		return values(Arrays.asList(values));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> values(Iterable<?> values) {
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
