package com.googlecode.objectify.impl.ref;

import java.util.Iterator;

import com.googlecode.objectify.Ref;


/**
 * <p>A proxy for the first ref in an Iterator.  Allows the iterator to be an async proxy, so the
 * query can execute until our first call.  If there is no first item in the iterator, proxies to
 * a StdRef(null, null).</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FirstRef<T> extends ProxyRef<T>
{
	private static final long serialVersionUID = 1L;
	
	/** */
	Iterator<Ref<T>> it;
	
	/** */
	public FirstRef(Iterator<Ref<T>> it) {
		this.it = it;
	}
	
	@Override
	protected Ref<T> ref() {
		if (it.hasNext())
			return it.next();
		else
			return new NullRef<T>();
	}
}