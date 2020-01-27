package com.googlecode.objectify.impl;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.ReadOption;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.Objectify;
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
class LoaderImpl extends Queryable<Object> implements Loader
{
	/** Used by some child command objects */
	final ObjectifyImpl ofy;

	/** */
	private final LoadArrangement loadArrangement;

	/** */
	private final ImmutableSet<ReadOption> readOptions;

	/** */
	LoaderImpl(final ObjectifyImpl ofy) {
		this(ofy, new LoadArrangement(), ImmutableSet.of());
	}

	/** */
	private LoaderImpl(final ObjectifyImpl ofy, final LoadArrangement loadArrangement, final ImmutableSet<ReadOption> readOptions) {
		super(null);
		this.ofy = ofy;
		this.loadArrangement = loadArrangement;
		this.readOptions = readOptions;
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
	public Loader group(final Class<?>... groups) {
		final LoadArrangement arrangement = new LoadArrangement();
		arrangement.addAll(Arrays.asList(groups));
		arrangement.addAll(this.loadArrangement);

		return new LoaderImpl(ofy, arrangement, readOptions);
	}

	@Override
	public Loader option(final ReadOption... options) {
		final ImmutableSet<ReadOption> newOptions = ImmutableSet.<ReadOption>builder()
				.addAll(readOptions)
				.addAll(Arrays.asList(options))
				.build();
		return new LoaderImpl(ofy, loadArrangement, newOptions);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#type(java.lang.Class)
	 */
	@Override
	public <E> LoadType<E> type(final Class<E> type) {
		return new LoadTypeImpl<>(this, Key.getKind(type), type);
	}

	@Override
	public <E> LoadType<E> kind(final String kind) {
		return new LoadTypeImpl<>(this, kind, null);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#ref(com.googlecode.objectify.Ref)
	 */
	@Override
	public <E> LoadResult<E> ref(final Ref<E> ref) {
		return key(ref.key());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#refs(com.googlecode.objectify.Ref<?>[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> refs(final Ref<? extends E>... refs) {
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
	public <E> LoadResult<E> key(final Key<E> key) {
		return new LoadResult<>(key, createLoadEngine().load(key));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entity(java.lang.Object)
	 */
	@Override
	public <E> LoadResult<E> entity(final E entity) {
		return key(ofy.factory().keys().keyOf(entity, ofy.getOptions().getNamespace()));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#value(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> LoadResult<E> value(final Object key) {
		return (LoadResult<E>)key(ofy.factory().keys().anythingToKey(key, ofy.getOptions().getNamespace()));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(com.googlecode.objectify.Key<E>[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> keys(final Key<? extends E>... keys) {
		return this.keys(Arrays.asList((Key<E>[])keys));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#keys(java.lang.Iterable)
	 */
	@Override
	public <E> Map<Key<E>, E> keys(final Iterable<Key<E>> keys) {
		return values(keys);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(E[])
	 */
	@Override
	public <E> Map<Key<E>, E> entities(final E... entities) {
		return this.entities(Arrays.asList(entities));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#entities(java.lang.Iterable)
	 */
	@Override
	public <E> Map<Key<E>, E> entities(final Iterable<E> values) {
		return values(values);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Object[])
	 */
	@Override
	public <E> Map<Key<E>, E> values(final Object... values) {
		return values(Arrays.asList(values));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#values(java.lang.Iterable)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <E> Map<Key<E>, E> values(final Iterable<?> values) {

		// Do this in a separate pass so any errors converting keys will show up before we try loading something
		final List<Key<E>> keys = new ArrayList<>();
		for (final Object keyish: values)
			keys.add(ofy.factory().keys().anythingToKey(keyish, ofy.getOptions().getNamespace()));

		final LoadEngine engine = createLoadEngine();

		final Map<Key<E>, Result<E>> results = new LinkedHashMap<>();
		for (final Key<E> key: keys)
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
							Maps.transformValues(results, ResultNowFunction.instance()),
							Predicates.notNull()));
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#getObjectify()
	 */
	@Override
	public Objectify getObjectify() {
		return ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#getLoadGroups()
	 */
	@Override
	public Set<Class<?>> getLoadGroups() {
		return Collections.unmodifiableSet(loadArrangement);
	}

	/** */
	public ObjectifyImpl getObjectifyImpl() {
		return this.ofy;
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for load commands
	 */
	LoadEngine createLoadEngine() {
		return new LoadEngine(ofy, ofy.getSession(), ofy.asyncDatastore(), loadArrangement, readOptions);
	}

	/**
	 * Use this once for one operation and then throw it away
	 * @return a fresh engine that handles fundamental datastore operations for queries
	 */
	QueryEngine createQueryEngine() {
		return new QueryEngine(this, ofy.asyncDatastore());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Loader#now(com.googlecode.objectify.Key)
	 */
	@Override
	public <E> E now(final Key<E> key) {
		return createLoadEngine().load(key).now();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Objectify#toPojo(com.google.cloud.datastore.Entity)
	 */
	@Override
	public <T> T fromEntity(final Entity entity) {
		final LoadEngine engine = createLoadEngine();
		final LoadContext context = new LoadContext(engine);
		final T result = engine.load(entity, context);
 		context.done();
	 	return result;
	}
}
