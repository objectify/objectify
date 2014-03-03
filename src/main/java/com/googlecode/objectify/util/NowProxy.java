package com.googlecode.objectify.util;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * What ResultProxy will serialize to, eliminates serialization of the Result and whatever extra complexity it contains.
 */
class NowProxy<T> implements InvocationHandler, Serializable
{
	private static final long serialVersionUID = 1L;
	
	T thing;
	
	public NowProxy(T thing) {
		this.thing = thing;
	}
	
	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return method.invoke(thing, args);
	}
}