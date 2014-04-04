package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadResult;
import com.googlecode.objectify.cmd.LoadIds;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.util.DatastoreUtils;
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
	Class<T> type;

	/** Possible parent */
	Key<T> parent;

	/**
	 */
	LoadTypeImpl(LoaderImpl<?> loader, Class<T> type) {
		super(loader);
		this.type = type;
	}

	/** */
	LoadTypeImpl(LoaderImpl<?> loader, Class<T> type, Key<T> parent) {
		this(loader, type);
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.cmd.QueryCommonImpl#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return new QueryImpl<T>(loader, type);
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
	public LoadResult<T> id(long id) {
		return loader.key(Key.create(parent, type, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#id(java.lang.String)
	 */
	@Override
	public LoadResult<T> id(String id) {
		return loader.key(Key.create(parent, type, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(Long[])
	 */
	@Override
	public Map<Long, T> ids(Long... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(java.lang.String[])
	 */
	@Override
	public Map<String, T> ids(String... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(java.lang.Iterable)
	 */
	@Override
	public <S> Map<S, T> ids(Iterable<S> ids) {

		final Map<Key<T>, S> keymap = new LinkedHashMap<Key<T>, S>();
		for (S id: ids)
			keymap.put(DatastoreUtils.createKey(parent, type, id), id);

		final Map<Key<T>, T> loaded = loader.keys(keymap.keySet());

		return ResultProxy.create(Map.class, new ResultCache<Map<S, T>>() {
			@Override
			protected Map<S, T> nowUncached() {
				Map<S, T> proper = new LinkedHashMap<S, T>(loaded.size() * 2);

				for (Map.Entry<Key<T>, T> entry: loaded.entrySet())
					proper.put(keymap.get(entry.getKey()), entry.getValue());

				return proper;
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadType#parent(java.lang.Object)
	 */
	@Override
	public LoadIds<T> parent(Object keyOrEntity) {
		Key<T> parentKey = loader.ofy.factory().keys().anythingToKey(keyOrEntity);
		return new LoadTypeImpl<T>(loader, type, parentKey);
	}

}
