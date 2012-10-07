package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.LoadIds;
import com.googlecode.objectify.cmd.LoadType;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.impl.Keys;
import com.googlecode.objectify.impl.engine.LoadEngine;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.ResultProxy;
import com.googlecode.objectify.util.ResultTranslator;


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
	LoadTypeImpl(LoaderImpl loader, Class<T> type) {
		super(loader);
		this.type = type;
	}

	/** */
	LoadTypeImpl(LoaderImpl loader, Class<T> type, Key<T> parent) {
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
	public Ref<T> id(long id) {
		return refOf(Key.create(parent, type, id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#id(java.lang.String)
	 */
	@Override
	public Ref<T> id(String id) {
		return refOf(Key.create(parent, type, id));
	}

	/** Utility method */
	private Ref<T> refOf(Key<T> key) {
		Ref<T> ref = Ref.create(key);
		loader.ref(ref);
		return ref;
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
		Map<S, Ref<T>> refs = new LinkedHashMap<S, Ref<T>>();

		LoadEngine batch = loader.createLoadEngine();

		for (S id: ids) {
			Key<T> key = DatastoreUtils.createKey(parent, type, id);
			refs.put(id, batch.getRef(key));
		}

		batch.execute();

		return ResultProxy.create(Map.class, new ResultTranslator<Map<S, Ref<T>>, Map<S, T>>(refs) {
			@Override
			protected Map<S, T> translate(Map<S, Ref<T>> from) {
				Map<S, T> proper = new LinkedHashMap<S, T>(from.size() * 2);

				for (Map.Entry<S, Ref<T>> entry: from.entrySet())
					if (entry.getValue().get() != null)
						proper.put(entry.getKey(), entry.getValue().get());

				return proper;
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadType#parent(java.lang.Object)
	 */
	@Override
	public LoadIds<T> parent(Object keyOrEntity) {
		Key<T> parentKey = Keys.toKey(keyOrEntity);
		return new LoadTypeImpl<T>(loader, type, parentKey);
	}

}
