package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.Result;


/**
 * <p>An implementation of Ref that works like a proxy.  We can't create dynamic proxies on abstract classes
 * and we don't want to suck in a bytecode manipulator dependency so we have to create this by hand.  No big deal.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ProxyRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;

	/** */
	Ref<T> cached;
	
	/** Implement this to provide the real, non-proxied Ref */
	abstract protected Ref<T> ref();
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#key()
	 */
	@Override
	final public Key<T> key() {
		return getRef().key();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	final public T get() {
		return getRef().get();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#getValue()
	 */
	@Override
	public T getValue() {
		return getRef().getValue();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#set(com.googlecode.objectify.Result)
	 */
	@Override
	final public void set(Result<T> value) {
		getRef().set(value);
	}
	
	/** Checks for the cached instance, if not there get it from ref() */
	private final Ref<T> getRef() {
		if (this.cached == null)
			this.cached = ref();
		
		return this.cached;
	}
}