package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;
import lombok.RequiredArgsConstructor;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


/**
 * A dynamic proxy that wraps a Result<?> value.  For example, if you had a Result<List<String>>, the
 * proxy would implement List<String> and call through to the inner object.
 */
@RequiredArgsConstructor
public class ResultProxy<T> implements InvocationHandler, Serializable
{
	/**
	 * Create a ResultProxy for the given interface.
	 */
	@SuppressWarnings("unchecked")
	public static <S> S create(Class<? super S> interf, Result<S> result) {
		return (S)Proxy.newProxyInstance(result.getClass().getClassLoader(), new Class[] { interf }, new ResultProxy<>(result));
	}

	private final Result<T> result;

	@Override
	public Object invoke(Object obj, Method meth, Object[] params) throws Throwable {
		return meth.invoke(result.now(), params);
	}
	
	private Object writeReplace() throws ObjectStreamException {
        return new NowProxy<>(result.now());
    }
	
}
