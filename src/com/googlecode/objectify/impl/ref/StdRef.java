package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.ResultNow;


/**
 * <p>Standard implementation of a Ref which holds a key and (optionally) a value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class StdRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;
	
	/** The key associated with this ref */
	protected Key<T> key;

	/** The value associated with the key */
	protected Result<T> result;
	
	/** For GWT serialization */
	protected StdRef() {}

	/** Create a Ref based on the key, no value initialized */
	public StdRef(Key<T> key) {
		this.key = key;
	}
	
	/** Create a Ref based on the key */
	public StdRef(Key<T> key, T value) {
		this.key = key;
		this.result = new ResultNow<T>(value);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	public Key<T> key() {
		return key;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	public T get() {
		if (this.result == null)
			this.result = makeResult();

		return this.result.now();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#set(com.googlecode.objectify.Result)
	 */
	public void set(Result<T> result) {
		this.result = result;
	}

	/**
	 * The default version of this just throws an exception because the StdRef can't make a result by itself.
	 * Subclasses might override it to do something fancier, especially if they have a link to the datastore.
	 */
	protected Result<T> makeResult() {
		throw new IllegalStateException("Ref<?> value has not been initialized");
	}
}