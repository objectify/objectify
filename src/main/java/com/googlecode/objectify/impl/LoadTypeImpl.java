package com.googlecode.objectify.impl;

import com.google.cloud.datastore.StructuredQuery.Filter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cmd.LoadIds;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.util.ResultCache;
import com.googlecode.objectify.util.ResultProxy;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;


/**
 * Implementation of the LoadType interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class LoadTypeImpl<T> extends Queryable<T> implements LoadType<T>
{
	/** */
	private final String kind;

	/** Might be null; perhaps we specified a raw kind only */
	private final Class<T> type;

	/** Possible parent */
	private final Key<T> parent;

	/**
	 */
	LoadTypeImpl(LoaderImpl loader, String kind, Class<T> type) {
		this(loader, kind, type, null);
	}

	/** */
	LoadTypeImpl(LoaderImpl loader, String kind, Class<T> type, Key<T> parent) {
		super(loader);
		this.kind = kind;
		this.type = type;
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryCommonImpl#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return new QueryImpl<>(loader, kind, type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#filter(java.lang.String, java.lang.Object)
	 */
	@Override
	public Query<T> filter(String condition, Object value) {
		QueryImpl<T> q = createQuery();
		q.addFilter(condition, value);
		return q;
	}

	/* */
	@Override
	public Query<T> filter(final Filter filter) {
		final QueryImpl<T> q = createQuery();
		q.addFilter(filter);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.Query#order(java.lang.String)
	 */
	@Override
	public Query<T> order(String condition) {
		QueryImpl<T> q = createQuery();
		q.addOrder(condition);
		return q;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#id(long)
	 */
	@Override
	public LoadResult<T> id(final long id) {
		return loader.key(this.makeKey(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#id(java.lang.String)
	 */
	@Override
	public LoadResult<T> id(final String id) {
		return loader.key(this.makeKey(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(Long[])
	 */
	@Override
	public Map<Long, T> ids(final Long... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(java.lang.String[])
	 */
	@Override
	public Map<String, T> ids(final String... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(java.lang.Iterable)
	 */
	@Override
	public <S> Map<S, T> ids(final Iterable<S> ids) {

		final Map<Key<T>, S> keymap = new LinkedHashMap<>();
		for (final S id: ids)
			keymap.put(this.makeKey(id), id);

		final Map<Key<T>, T> loaded = loader.keys(keymap.keySet());

		return ResultProxy.create(Map.class, new ResultCache<Map<S, T>>() {
			@Override
			protected Map<S, T> nowUncached() {
				final Map<S, T> proper = new LinkedHashMap<>(loaded.size() * 2);

				for (final Map.Entry<Key<T>, T> entry: loaded.entrySet())
					proper.put(keymap.get(entry.getKey()), entry.getValue());

				return proper;
			}
		});
	}

	/**
	 * Make a key for the given id, which could be either string or long
	 */
	private <T> Key<T> makeKey(final Object id) {
		final com.google.cloud.datastore.Key key = factory().keys().createRawAny(
				loader.ofy.getOptions().getNamespace(),
				Keys.raw(this.parent),
				kind,
				id);
		return Key.create(key);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadType#parent(java.lang.Object)
	 */
	@Override
	public LoadIds<T> parent(final Object keyOrEntity) {
		final Key<T> parentKey = factory().keys().anythingToKey(keyOrEntity, loader.ofy.getOptions().getNamespace());
		return new LoadTypeImpl<>(loader, kind, type, parentKey);
	}

	/** */
	private ObjectifyFactory factory() {
		return loader.getObjectifyImpl().factory();
	}
}
