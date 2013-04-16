package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;


/**
 * <p>Standard implementation of Ref which holds a key.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class StdRef<T> extends LiveRef<T>
{
	private static final long serialVersionUID = 1L;

	/** The key associated with this ref */
	protected Key<T> key;

	/** For GWT serialization */
	protected StdRef() {}

	/**
	 * Create a Ref based on the key
	 */
	public StdRef(Key<T> key) {
		this(key, ObjectifyService.ofy());
	}

	/**
	 * Create a Ref based on the key, with the specified session
	 */
	public StdRef(Key<T> key, Objectify ofy) {
		super(ofy);

		if (key == null)
			throw new NullPointerException("Cannot create a Ref for a null key");

		this.key = key;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	public Key<T> key() {
		return key;
	}
}