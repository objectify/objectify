package com.googlecode.objectify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;

/**
 * Implementation of the ObjPreparedQuery interface.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OPreparedQueryImpl<T> implements OPreparedQuery<T>
{
	/** We always need one of these */
	ObjectifyFactory factory;
	
	/** The backing result set */
	PreparedQuery pq;

	/** If true, this query will produce only keys, not entity objects */
	boolean keysOnly;

	/** Wrap the prepared query */
	protected OPreparedQueryImpl(ObjectifyFactory fact, PreparedQuery pq, boolean keysOnly)
	{
		this.factory = fact;
		this.pq = pq;
		this.keysOnly = keysOnly;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asIterable()
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<T> asIterable()
	{
		return (Iterable<T>)new ToObjectIterable(this.pq.asIterable());
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asIterable(com.google.appengine.api.datastore.FetchOptions)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public Iterable<T> asIterable(FetchOptions fetchOptions)
	{
		return (Iterable<T>)new ToObjectIterable(this.pq.asIterable(fetchOptions));
	}

	/*
	 * (non-Javadoc)
	 * @see com.googlecode.objectify.ObjPreparedQuery#asList()
	 */
	@SuppressWarnings("unchecked")
	public List<T> asList()
	{
		List<T> resultList = new ArrayList<T>();

		for (T obj: (Iterable<T>)new ToObjectIterable(this.pq.asIterable()))
			resultList.add(obj);

		return resultList;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asList(com.google.appengine.api.datastore.FetchOptions)
	 */
	@SuppressWarnings("unchecked")
	public List<T> asList(FetchOptions fetchOptions)
	{
		List<Entity> entityList = this.pq.asList(fetchOptions);
		List<T> resultList = new ArrayList<T>(entityList.size());

		for (T obj: (Iterable<T>)new ToObjectIterable(entityList))
			resultList.add(obj);

		return resultList;
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#asSingleEntity()
	 */
	@SuppressWarnings("unchecked")
	public T asSingle()
	{
		Entity ent = this.pq.asSingleEntity();
		if (ent == null)
			return null;

		if (this.keysOnly)
		{
			return (T)this.factory.rawKeyToOKey(ent.getKey());
		}
		else
		{
			EntityMetadata metadata = this.factory.getMetadata(ent.getKey());
			return (T)metadata.toObject(ent);
		}
	}

	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#countEntities()
	 */
	public int count()
	{
		return this.pq.countEntities();
	}

	/**
	 * Iterable that translates from datastore Entity to types Objects
	 */
	class ToObjectIterable<S> implements Iterable<S>
	{
		Iterable<Entity> source;
		boolean keysOnly;

		public ToObjectIterable(Iterable<Entity> source)
		{
			this.source = factory.maybeWrap(source);
		}

		@Override
		public Iterator<S> iterator()
		{
			return new ToObjectIterator<S>(this.source.iterator());
		}

	}

	/**
	 * Iterator that translates from datastore Entity to typed Objects
	 */
	class ToObjectIterator<S> implements Iterator<S>
	{
		Iterator<Entity> source;

		public ToObjectIterator(Iterator<Entity> source)
		{
			this.source = factory.maybeWrap(source);
		}

		@Override
		public boolean hasNext()
		{
			return this.source.hasNext();
		}

		@Override
		@SuppressWarnings("unchecked")
		public S next()
		{
			Entity nextEntity = this.source.next();
			if (keysOnly)
			{
				// This will be a ToObjectIterator<Key<T>>
				return (S)factory.rawKeyToOKey(nextEntity.getKey());
			}
			else
			{
				EntityMetadata<S> meta = factory.getMetadata(nextEntity.getKey());
				return meta.toObject(nextEntity);
			}
		}

		@Override
		public void remove()
		{
			this.source.remove();
		}

	}
}