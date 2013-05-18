package com.googlecode.objectify.impl;

import java.util.Arrays;
import java.util.Collections;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteIds;
import com.googlecode.objectify.cmd.DeleteType;
import com.googlecode.objectify.util.DatastoreUtils;


/**
 * Implementation of the DeleteType and DeleteIds interfaces.  No need for separate implementations.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeleteTypeImpl implements DeleteType
{
	/** */
	DeleterImpl deleter;

	/** Translated from the type class */
	Class<?> type;

	/** Possible parent */
	Key<?> parent;

	/**
	 * @param type must be a registered type
	 */
	DeleteTypeImpl(DeleterImpl deleter, Class<?> type) {
		this.deleter = deleter;
		this.type = type;
	}

	/**
	 * @param parent can be Key, Key<?>, or entity
	 */
	DeleteTypeImpl(DeleterImpl deleter, Class<?> type, Key<?> parent) {
		this(deleter, type);
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteType#parent(java.lang.Object)
	 */
	@Override
	public DeleteIds parent(Object keyOrEntity) {
		Key<?> parentKey = Keys.toKey(keyOrEntity);
		return new DeleteTypeImpl(deleter, type, parentKey);
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
		return this.deleter.keys(DatastoreUtils.createKeys(parent, type, ids));
	}

}
