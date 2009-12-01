package com.googlecode.objectify;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;


/**
 * Nearly identical to the native datastore Query object, but works with
 * typed classes instead of kinds. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjQuery
{
	/** All of the methods on this object simply call through to the underlying Query object */
	Query impl;
	
	/**
	 * Creates a new kind-less query that finds entities.
	 * @see Query#Query()
	 */
	public ObjQuery()
	{
		this.impl = new Query();
	}
	
	/**
	 * Creates a query that finds entities with the specified ancestor
	 * @see Query#Query(Key)
	 */
	public ObjQuery(Key ancestor)
	{
		this.impl = new Query(ancestor);
	}
	
	/**
	 * Creates a query that finds entities with the specified type
	 * @see Query#Query(String)
	 */
	public ObjQuery(Class<?> entityClazz)
	{
		this.impl = new Query(ObjectifyFactory.getKind(entityClazz));
	}
	
	/**
	 * Creates a query that finds entities with the specified type and ancestor
	 * @see Query#Query(String, Key)
	 */
	public ObjQuery(Class<?> entityClazz, Key ancestor)
	{
		this.impl = new Query(ObjectifyFactory.getKind(entityClazz), ancestor);
	}
	
	/**
	 * @return the underlying Query equivalent object
	 */
	public Query toQuery()
	{
		return this.impl;
	}
}