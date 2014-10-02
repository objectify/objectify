package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.DeferredDeleteIds;
import com.googlecode.objectify.cmd.DeferredDeleteType;
import com.googlecode.objectify.util.DatastoreUtils;
import java.util.Arrays;
import java.util.Collections;


/**
 * Implementation of the DeferredDeleteType and DeferredDeleteIds interfaces.  No need for separate implementations.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
class DeferredDeleteTypeImpl implements DeferredDeleteType
{
	/** */
	DeferredDeleterImpl deleter;

	/** Translated from the type class */
	Class<?> type;

	/** Possible parent */
	Key<?> parent;

	/**
	 * @param type must be a registered type
	 */
	DeferredDeleteTypeImpl(DeferredDeleterImpl deleter, Class<?> type) {
		this.deleter = deleter;
		this.type = type;
	}

	/**
	 * @param parent can be Key, Key<?>, or entity
	 */
	DeferredDeleteTypeImpl(DeferredDeleterImpl deleter, Class<?> type, Key<?> parent) {
		this(deleter, type);
		this.parent = parent;
	}

	@Override
	public DeferredDeleteIds parent(Object keyOrEntity) {
		Key<?> parentKey = deleter.ofy.factory().keys().anythingToKey(keyOrEntity);
		return new DeferredDeleteTypeImpl(deleter, type, parentKey);
	}

	@Override
	public void id(long id) {
		ids(Collections.singleton(id));
	}

	@Override
	public void id(String id) {
		ids(Collections.singleton(id));
	}

	@Override
	public void ids(long... ids) {
		ids(Arrays.asList(ids));
	}

	@Override
	public void ids(String... ids) {
		ids(Arrays.asList(ids));
	}

	@Override
	public void ids(Iterable<?> ids) {
		this.deleter.keys(DatastoreUtils.createKeys(parent, type, ids));
	}

}
