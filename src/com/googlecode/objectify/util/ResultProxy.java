package com.googlecode.objectify.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.Result;


/**
 * A dynamic proxy that wraps a Result<?> value.  For example, if you had a Result<List<String>>, the
 * proxy would implement List<String> and call through to the inner object.
 */
public class ResultProxy<T> implements InvocationHandler
{
	/**
	 * Converts an Iterable into a list asynchronously, using the ResultProxy. 
	 */
	@SuppressWarnings("unchecked")
	public static <S> List<S> makeAsyncList(Iterable<S> it) {
		Result<List<S>> result = new ResultTranslator<Iterable<S>, List<S>>(it) {
			@Override
			protected List<S> translate(Iterable<S> from) {
				List<S> list = new ArrayList<S>();
				for (S s: from)
					list.add(s);
				
				return list;
			}
		};
		
		return create(List.class, result);
	}

	/**
	 * Create a ResultProxy for the given interface.
	 */
	@SuppressWarnings("unchecked")
	public static <T, I extends T> T create(Class<I> interf, Result<T> result) {
		return (T)Proxy.newProxyInstance(result.getClass().getClassLoader(), new Class[] { interf }, new ResultProxy<T>(result));
	}
	
	Result<T> result;
	
	private ResultProxy(Result<T> result) {
		this.result = result;
	}

	@Override
	public Object invoke(Object obj, Method meth, Object[] params) throws Throwable {
		return meth.invoke(result.now(), params);
	}
}
