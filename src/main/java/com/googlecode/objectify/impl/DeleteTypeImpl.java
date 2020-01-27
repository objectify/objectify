package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.cmd.DeleteIds;
import com.googlecode.objectify.cmd.DeleteType;
import com.googlecode.objectify.util.ArrayUtils;

import java.util.Arrays;
import java.util.Collections;


/**
 * Implementation of the DeleteType and DeleteIds interfaces.  No need for separate implementations.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeleteTypeImpl implements DeleteType
{
	/** */
	private final DeleterImpl deleter;

	/** Translated from the type class */
	private final Class<?> type;

	/** Possible parent */
	private final Key<?> parent;

	/**
	 * @param type must be a registered type
	 */
	DeleteTypeImpl(final DeleterImpl deleter, final Class<?> type) {
		this(deleter, type, null);
	}

	/**
	 * @param parent can be Key, Key<?>, or entity
	 */
	DeleteTypeImpl(final DeleterImpl deleter, final Class<?> type, final Key<?> parent) {
		this.deleter = deleter;
		this.type = type;
		this.parent = parent;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteType#parent(java.lang.Object)
	 */
	@Override
	public DeleteIds parent(final Object keyOrEntity) {
		final Key<?> parentKey = factory().keys().anythingToKey(keyOrEntity, deleter.ofy.getOptions().getNamespace());
		return new DeleteTypeImpl(deleter, type, parentKey);
	}

	private ObjectifyFactory factory() {
		return deleter.ofy.factory();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#id(long)
	 */
	@Override
	public Result<Void> id(final long id) {
		return ids(Collections.singleton(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#id(java.lang.String)
	 */
	@Override
	public Result<Void> id(final String id) {
		return ids(Collections.singleton(id));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(long[])
	 */
	@Override
	public Result<Void> ids(final long... ids) {
		return ids(ArrayUtils.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(java.lang.String[])
	 */
	@Override
	public Result<Void> ids(final String... ids) {
		return ids(Arrays.asList(ids));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.cmd.DeleteIds#ids(java.lang.Iterable)
	 */
	@Override
	public <S> Result<Void> ids(final Iterable<S> ids) {
		return this.deleter.keys(factory().keys().createKeys(deleter.ofy.getOptions().getNamespace(), parent, type, ids));
	}

}
