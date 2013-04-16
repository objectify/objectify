package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.impl.engine.QueryEngine;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultNowFunction;
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
	public <E> Ref<E> ref(Ref<E> ref) {
		return key(ref.key());
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
		return values(refs);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(com.googlecode.objectify.Key)
	 */
	@Override
	public <E> Ref<E> key(Key<E> key) {
		return createLoadEngine().getRef(key);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(java.lang.Object)
	 */
	@Override
	public <E> Ref<E> entity(E entity) {
		return key(Key.create(entity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#value(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Ref<E> value(Object key) {
		return (Ref<E>)key(Keys.toKey(key));
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

		// Do this in a separate pass so any errors converting keys will show up before we try loading something
		List<Key<E>> keys = new ArrayList<Key<E>>();
		for (Object keyish: values)
			keys.add((Key<E>)Keys.toKey(keyish));

		LoadEngine engine = createLoadEngine();

		final Map<Key<E>, Result<E>> results = new LinkedHashMap<Key<E>, Result<E>>();
		for (Key<E> key: keys)
			results.put(key, engine.load(key));

		engine.execute();

		// Now asynchronously translate into a normal-looking map. We must be careful to exclude results with
		// null (missing) values because that is the contract established by DatastoreService.get().
		// We use the ResultProxy and create a new map because the performance of filtered views is questionable.
		return ResultProxy.create(Map.class, new ResultCache<Map<Key<E>, E>>() {
			@Override
			public Map<Key<E>, E> nowUncached() {
				return
					Maps.newLinkedHashMap(
						Maps.filterValues(
							Maps.transformValues(results, ResultNowFunction.<E>instance()),
							Predicates.notNull()));
			}
		});
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

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#now(com.googlecode.objectify.Key)
	 */
	@Override
	public <E> E now(Key<E> key) {
		return createLoadEngine().load(key).now();
	}

}
