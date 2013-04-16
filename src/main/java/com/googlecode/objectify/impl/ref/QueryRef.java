package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;


/**
 * <p>Implementation of Ref for the query.first(). Unlike normal Refs, this one can produce a null key.
 * However, like normal Refs it can load the entity - even if it came from a keys-only query.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryRef<T> extends LiveRef<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 */
	final Result<Key<T>> key;

	/**
	 */
	public QueryRef(Result<Key<T>> key) {
		this.key = key;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	final public Key<T> key() {
		return key.now();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	final public T get() {
		return key.now() != null ? super.get() : null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#isLoaded()
	 */
	@Override
	public boolean isLoaded() {
		return key.now() != null ? super.isLoaded() : false;
	}
}