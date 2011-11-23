package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Serialize;

/** 
 * Loadable which encapsulates a method with an @AlsoLoad parameter. 
 * If you try to get() the value it is always null.
 */
public class LoadableMethod implements Loadable
{
	String[] names;
	Method method;
	
	boolean embed;
	boolean serialize;
	
	public LoadableMethod(Method method, AlsoLoad annotation) {
		this.method = method;
		method.setAccessible(true);

		// Method must have only one parameter
		if (method.getParameterTypes().length != 1)
			throw new IllegalStateException("@AlsoLoad methods must have a single parameter. Can't use " + method);
		
		if (method.getParameterTypes()[0].isAnnotationPresent(Embed.class))
			embed = true;
		
		if (method.getParameterTypes()[0].isAnnotationPresent(Serialize.class))
			serialize = true;

		for (Annotation anno: method.getParameterAnnotations()[0]) {
			if (anno instanceof Embed)
				embed = true;
			else if (anno instanceof Serialize)
				serialize = true;
			
			if (embed && serialize)
				throw new IllegalStateException("You cannot specify both @Serialize and @Embed on " + method);
		}
		
		if (annotation.value() == null || annotation.value().length == 0)
			throw new IllegalStateException("@AlsoLoad must have a value on " + method);
		
		Set<String> nameSet = new HashSet<String>();
		
		for (String name: annotation.value()) {
			if (name == null || name.trim().length() == 0)
				throw new IllegalStateException("Illegal empty value in @AlsoLoad for " + method);
			
			nameSet.add(name);
		}
		
		names = nameSet.toArray(new String[nameSet.size()]);
	}
	
	@Override
	public String[] getNames() { return names; }
	
	@Override
	public Type getType() { return this.method.getGenericParameterTypes()[0]; }
	
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
	public boolean isSerialize()
	{
		return serialize;
	}
	
	@Override
	public boolean isEmbed()
	{
		return embed;
	}
	
	@Override
	public String toString()
	{
		return this.method.toString();
	}

}