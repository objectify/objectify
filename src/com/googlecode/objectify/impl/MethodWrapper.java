package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import com.googlecode.objectify.annotation.Serialized;

/** 
 * Wrapper which makes a method with a single parameter look just like a field.
 * Well, almost - if you try to get() the value it is always null.
 */
public class MethodWrapper implements Wrapper
{
	Method method;
	
	public MethodWrapper(Method method) { this.method = method; }
	
	@Override
	public Class<?> getType() { return this.method.getParameterTypes()[0]; }
	
	@Override
	public Type getGenericType() { return this.method.getGenericParameterTypes()[0]; }
	
	@Override
	public void set(Object pojo, Object value)
	{
		try { this.method.invoke(pojo, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
		catch (InvocationTargetException ex) { throw new RuntimeException(ex); }
	}
	
	@Override
	public Object get(Object pojo)
	{
		return null;	// can't get values from methods
	}
	
	@Override
	public boolean isSerialized()
	{
		Annotation[] annos = this.method.getParameterAnnotations()[0];
		for (Annotation anno: annos)
			if (anno instanceof Serialized)
				return true;
		
		return false;
	}
	
	@Override
	public String toString()
	{
		return this.method.toString();
	}
}