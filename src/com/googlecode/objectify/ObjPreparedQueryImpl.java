package com.googlecode.objectify;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;

/**
 * Implementation of the ObjPreparedQuery interface.
 */
public class ObjPreparedQueryImpl<T> implements ObjPreparedQuery<T>
{
	/** The backing result set */
	PreparedQuery pq;
	
	/** Wrap the prepared query */
	public ObjPreparedQueryImpl(PreparedQuery pq)
	{
		this.pq = pq;
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
	public T asSingleEntity()
	{
		Entity ent = this.pq.asSingleEntity();
		EntityMetadata metadata = ObjectifyFactory.getMetadata(ent.getKey());
		return (T)metadata.toObject(ent);
	}
	
	/* (non-Javadoc)
	 * @see com.google.code.objectify.ObjPreparedQuery#countEntities()
	 */
	public int countEntities()
	{
		return this.pq.countEntities();
	}
	
	/**
	 * Iterable that translates from datastore Entity to types Objects
	 */
	static class ToObjectIterable implements Iterable<Object>
	{
		Iterable<Entity> source;
		
		public ToObjectIterable(Iterable<Entity> source)
		{
			this.source = source;
		}

		@Override
		public Iterator<Object> iterator()
		{
			return new ToObjectIterator(this.source.iterator());
		}
		
	}
	
	/**
	 * Iterator that translates from datastore Entity to typed Objects
	 */
	static class ToObjectIterator implements Iterator<Object>
	{
		Iterator<Entity> source;
		
		public ToObjectIterator(Iterator<Entity> source)
		{
			this.source = source;
		}

		@Override
		public boolean hasNext()
		{
			return this.source.hasNext();
		}

		@Override
		public Object next()
		{
			Entity nextEntity = this.source.next();
			EntityMetadata meta = ObjectifyFactory.getMetadata(nextEntity.getKey());
			return meta.toObject(nextEntity);
		}

		@Override
		public void remove()
		{
			this.source.remove();
		}
		
	}
}