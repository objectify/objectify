package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;


/**
 * <p>Null key and value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NullRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	public Key<T> key() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	public T get() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#getValue()
	 */
	@Override
	public T getValue() {
		return null;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#set(com.googlecode.objectify.Result)
	 */
	public void set(Result<T> result) {
		throw new UnsupportedOperationException("Can't set the result of a null ref");
	}
}