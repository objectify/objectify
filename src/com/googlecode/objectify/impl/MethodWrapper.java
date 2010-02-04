package com.googlecode.objectify.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/** 
 * Wrapper which makes a method with a single parameter look just like a field.
 * Well, almost - if you try to get() the value it is always null.
 */
public class MethodWrapper implements Wrapper
{
	Method method;
	
	public MethodWrapper(Method method) { this.method = method; }
	
	public Class<?> getType() { return this.method.getParameterTypes()[0]; }
	
	public Type getGenericType() { return this.method.getGenericParameterTypes()[0]; }
	
	public void set(Object entity, Object value)
	{
		try { this.method.invoke(entity, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
		catch (InvocationTargetException ex) { throw new RuntimeException(ex); }
	}
	
	public Object get(Object entity)
	{
		return null;	// can't get values from methods
	}
}