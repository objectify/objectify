package com.googlecode.objectify.impl.cmd;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteIds;
import com.googlecode.objectify.cmd.DeleteType;


/**
 * Implementation of the DeleteType and DeleteIds interfaces.  No need for separate implementations.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeleteTypeImpl implements DeleteType
{
	/** */
	ObjectifyImpl ofy;
	
	/** Translated from the type class */
	String kind;
	
	/** Possible parent */
	com.google.appengine.api.datastore.Key parent;
	
	/**
	 * @param type must be a registered type 
	 */
	DeleteTypeImpl(ObjectifyImpl ofy, String kind) {
		this.ofy = ofy;
		this.kind = kind;
	}

	/**
	 * @param parent can be Key, Key<?>, or entity 
	 */
	DeleteTypeImpl(ObjectifyImpl ofy, String kind, com.google.appengine.api.datastore.Key parent) {
		this(ofy, kind);
		this.parent = ofy.getFactory().getRawKey(parent);

	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteType#parent(java.lang.Object)
	 */
	@Override
	public DeleteIds parent(Object keyOrEntity) {
		com.google.appengine.api.datastore.Key parentKey = ofy.getFactory().getRawKey(keyOrEntity);
		return new DeleteTypeImpl(ofy, kind, parentKey);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#id(long)
	 */
	@Override
	public Result<Void> id(long id) {
		return ids(Collections.singleton(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#id(java.lang.String)
	 */
	@Override
	public Result<Void> id(String id) {
		return ids(Collections.singleton(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(long[])
	 */
	@Override
	public Result<Void> ids(long... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(java.lang.String[])
	 */
	@Override
	public Result<Void> ids(String... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(java.lang.Iterable)
	 */
	@Override
	public <S> Result<Void> ids(Iterable<S> ids) {
		List<com.google.appengine.api.datastore.Key> keys = new ArrayList<com.google.appengine.api.datastore.Key>();
		
		for (Object id: ids) {
			if (id instanceof String)
				keys.add(KeyFactory.createKey(parent, kind, (String)id));
			else if (id instanceof Long)
				keys.add(KeyFactory.createKey(parent, kind, (Long)id));
			else
				throw new IllegalArgumentException("Id + '" + id + "' must be String or Long");
		}
		
		return ofy.delete(keys);
	}

}
