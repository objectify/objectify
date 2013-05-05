package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;


/**
 * <p>Implementation of Ref which is disconnected from the live system; for example, this will be used
 * if a Ref gets serialized or deserialized.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DeadRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;

	/** */
	T value;

	/** For GWT serialization */
	protected DeadRef() {}

	/**
	 * Create a Ref based on the key
	 */
	public DeadRef(Key<T> key) {
		this(key, null);
	}

	/**
	 * Create a Ref based on the key and value
	 */
	public DeadRef(Key<T> key, T value) {
		super(key);
		this.value = value;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	public T get() {
		return value;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#isLoaded()
	 */
	@Override
	public boolean isLoaded() {
		return true;
	}
}