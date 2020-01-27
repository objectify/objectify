package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.cmd.DeferredDeleteIds;
import com.googlecode.objectify.cmd.DeferredDeleteType;
import com.googlecode.objectify.util.ArrayUtils;

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
	private final DeferredDeleterImpl deleter;

	/** Translated from the type class */
	private final Class<?> type;

	/** Possible parent */
	private final Key<?> parent;

	/**
	 * @param type must be a registered type
	 */
	DeferredDeleteTypeImpl(final DeferredDeleterImpl deleter, final Class<?> type) {
		this(deleter, type, null);
	}

	/**
	 * @param parent can be null for "no parent"
	 */
	DeferredDeleteTypeImpl(final DeferredDeleterImpl deleter, final Class<?> type, final Key<?> parent) {
		this.deleter = deleter;
		this.type = type;
		this.parent = parent;
	}

	@Override
	public DeferredDeleteIds parent(final Object keyOrEntity) {
		final Key<?> parentKey = factory().keys().anythingToKey(keyOrEntity, deleter.ofy.getOptions().getNamespace());
		return new DeferredDeleteTypeImpl(deleter, type, parentKey);
	}

	@Override
	public void id(final long id) {
		ids(Collections.singleton(id));
	}

	@Override
	public void id(final String id) {
		ids(Collections.singleton(id));
	}

	@Override
	public void ids(final long... ids) {
		ids(ArrayUtils.asList(ids));
	}

	@Override
	public void ids(final String... ids) {
		ids(Arrays.asList(ids));
	}

	@Override
	public void ids(final Iterable<?> ids) {
		this.deleter.keys(factory().keys().createKeys(deleter.ofy.getOptions().getNamespace(), parent, type, ids));
	}

	private ObjectifyFactory factory() {
		return deleter.ofy.factory();
	}

}
