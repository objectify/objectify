package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

/**
 * <p>GWT emulation of Ref is contained within Ref; this class is necessary only to keep serialization working.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LiveRef<T> extends Ref<T>
{
	/** Make GWT happy */
	protected LiveRef() {}

	/** */
	public LiveRef(Key<T> key) {
		super(key);
	}

	/** */
	public LiveRef(T value) {
		super(value);
	}

	/** */
	public LiveRef(Key<T> key, T value) {
		super(key, value);
	}
}