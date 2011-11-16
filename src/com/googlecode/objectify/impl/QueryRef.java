package com.googlecode.objectify.impl;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;


/**
 * <p>Ref which can be used as the result of a query, when the key is not known until the query completes.</p>
 * 
 * TODO:  Make this serialize into a StdRef
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class QueryRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;
	
	/** Needed to obtain a key from the result */
	protected Objectify ofy;
	
	/** For GWT serialization */
	protected QueryRef() {}

	/** Create a Ref based on the result */
	public QueryRef(Result<T> result, Objectify ofy) {
		this.result = result;
		this.ofy = ofy;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	public Key<T> key() {
		return ofy.getFactory().getKey(get());
	}
}