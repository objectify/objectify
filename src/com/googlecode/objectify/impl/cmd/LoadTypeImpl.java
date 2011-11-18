package com.googlecode.objectify.impl.cmd;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.cmd.LoadIds;
import com.googlecode.objectify.cmd.LoadType;
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
	LoadTypeImpl(ObjectifyImpl ofy, Set<String> fetchGroups, Class<T> type) {
		super(ofy, fetchGroups);
		this.type = type;
	}
	
	/** */
	LoadTypeImpl(ObjectifyImpl ofy, Set<String> fetchGroups, Class<T> type, Key<T> parent) {
		this(ofy, fetchGroups, type);
		this.parent = parent;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.FindTypeBase#createQuery()
	 */
	@Override
	QueryImpl<T> createQuery() {
		return new QueryImpl<T>(ofy, fetchGroups, type);
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
		ofy.load().ref(ref);
		return ref;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadIds#ids(long[])
	 */
	@Override
	public Map<Long, T> ids(long... ids) {
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
	public <S> Map<S, T> ids(Iterable<?> ids) {
		List<com.google.appengine.api.datastore.Key> keys = DatastoreUtils.createKeys(parent.getRaw(), Key.getKind(type), ids);
		
		Map<Key<T>, T> fetched = ofy.createGetEngine(fetchGroups).get(keys);
		
		return ResultProxy.create(Map.class, new ResultTranslator<Map<Key<T>, T>, Map<S, T>>(fetched) {
			@Override
			protected Map<S, T> translate(Map<Key<T>, T> from) {
				Map<S, T> primitive = new LinkedHashMap<S, T>();
				
				for (Map.Entry<Key<T>, T> entry: from.entrySet()) {
					
					@SuppressWarnings("unchecked")
					S key = (entry.getKey().getString() != null)
							? (S)entry.getKey().getString()
							: (S)(Long)entry.getKey().getId();
							
					primitive.put(key, entry.getValue());
				}
				
				return primitive;
			}
		});
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.LoadType#parent(java.lang.Object)
	 */
	@Override
	public LoadIds<T> parent(Object keyOrEntity) {
		Key<T> parentKey = ofy.getFactory().getKey(keyOrEntity);
		return new LoadTypeImpl<T>(ofy, fetchGroups, type, parentKey);
	}

}
