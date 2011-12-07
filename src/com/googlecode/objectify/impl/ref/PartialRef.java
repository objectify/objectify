package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.Result;


/**
 * <p>This is a kind of Ref that represents a key and a partial entity (just key fields set).  Since it's
 * mildly expensive to construct the partial we delay that until the last moment.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PartialRef<T> extends StdRef<T>
{
	private static final long serialVersionUID = 1L;
	
	/** Needed to obtain partial from the key */
	protected Objectify ofy;
	
	/** */
	public PartialRef(Key<T> key, Objectify ofy) {
		super(key);
		this.ofy = ofy;
	}

	@Override
	protected Result<T> makeResult() {
		// todo: fetch the value
		return null;
	}
}