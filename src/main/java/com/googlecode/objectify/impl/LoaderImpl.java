package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Loader;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultNowFunction;
import com.googlecode.objectify.util.ResultProxy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * <p>Implementation of the Loader interface. This is also suitable for subclassing; you
 * can return your own subclass by overriding ObjectifyImpl.load().</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LoaderImpl<L extends Loader> extends Queryable<Object> implements Loader, Cloneable
{
	/** */
	private ObjectifyImpl<?> ofy;
	private Transactor<?> transactor;

	/** */
	protected LoadArrangement loadArrangement = new LoadArrangement();

	/** */
	public LoaderImpl(ObjectifyImpl<?> ofy) {
		super(null);
		this.ofy = ofy;
		// We need to save off the transactor at the top of the stack when the loader is created, then use
		// the same transactor when actually executing the load. These were the original semantics when the
		// transaction context stack was maintained outside of ObjectifyImpl, so we need to preserve these semantics.
		this.transactor = ofy.transactor();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryDefinition#createQuery()
	 */
	@Override
	QueryImpl<Object> createQuery() {
		return new QueryImpl<>(this);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#group(java.lang.Class<?>[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public L group(Class<?>... groups) {
		LoaderImpl<L> clone = this.clone();

		clone.loadArrangement = new LoadArrangement();
		clone.loadArrangement.addAll(Arrays.asList(groups));
		clone.loadArrangement.addAll(this.loadArrangement);

		return (L)clone;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#type(java.lang.Class)
	 */
	@Override
	public <E> LoadType<E> type(Class<E> type) {
		return new LoadTypeImpl<>(this, Key.getKind(type), type);
	}

	@Override
	public <E> LoadType<E> kind(String kind) {
		return new LoadTypeImpl<>(this, kind, null);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#ref(com.googlecode.objectify.Ref)
	 */
	@Override
	public <E> LoadResult<E> ref(Ref<E> ref) {
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
	public <E> LoadResult<E> key(Key<E> key) {
		return new LoadResult<>(key, createLoadEngine().load(key));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(java.lang.Object)
	 */
	@Override
	public <E> LoadResult<E> entity(E entity) {
		return key(factory().keys().keyOf(entity));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#value(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> LoadResult<E> value(Object key) {
		return (LoadResult<E>)key(factory().keys().anythingToKey(key));
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
		List<Key<E>> keys = new ArrayList<>();
		for (Object keyish: values)
			keys.add((Key<E>)factory().keys().anythingToKey(keyish));

		LoadEngine engine = createLoadEngine();

		final Map<Key<E>, Result<E>> results = new LinkedHashMap<>();
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
	 * @see com.googlecode.objectify.cmd.Loader#getLoadGroups()
	 */
	@Override
	public Set<Class<?>> getLoadGroups() {
		return Collections.unmodifiableSet(loadArrangement);
	}

	/* Expose the factory but not expose ofy directly. */
	public ObjectifyFactory factory() {
		return ofy.factory();
	}

	/* Expose the cache setting but not expose ofy directly. */
	public boolean getCache() {
		return ofy.getCache();
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for load commands
	 */
	protected LoadEngine createLoadEngine() {
		return new LoadEngine(ofy, transactor, ofy.createAsyncDatastoreService(), loadArrangement);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for queries
	 */
	protected QueryEngine createQueryEngine() {
		return new QueryEngine(this, ofy.createAsyncDatastoreService(),
				transactor.getTransaction() == null ? null : transactor.getTransaction().getRaw());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#now(com.googlecode.objectify.Key)
	 */
	@Override
	public <E> E now(Key<E> key) {
		return createLoadEngine().load(key).now();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#toPojo(com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public <T> T fromEntity(Entity entity) {
		LoadEngine engine = createLoadEngine();
		LoadContext context = new LoadContext(engine);
		T result = engine.load(entity, context);
 		context.done();
	 	return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#clone()
	 */
	@SuppressWarnings("unchecked")
	protected LoaderImpl<L> clone() {
		try {
			return (LoaderImpl<L>)super.clone();
		} catch (CloneNotSupportedException e) {
			throw new RuntimeException(e); // impossible
		}
	}

}
