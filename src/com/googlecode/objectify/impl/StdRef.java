package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * <p>Standard implementation of a Ref, useful when you know the key in advance.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class StdRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;
	
	/** The key associated with this ref, known in advance */
	protected Key<T> key;

	/** For GWT serialization */
	protected StdRef() {}

	/** Create a Ref based on the key */
	public StdRef(Key<T> key) {
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