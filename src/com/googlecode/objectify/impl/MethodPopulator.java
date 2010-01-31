package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Works with methods
 */
class MethodPopulator implements Populator
{
	private final ObjectifyFactory factory;
	private final Method method;
	private final String name;
	private final Class<?> paramType;
	private Class<?> paramComponentType;

	MethodPopulator(ObjectifyFactory factory, String name, Method method)
	{
		this.factory = factory;
		this.name = name;
		this.method = method;
		this.paramType = method.getParameterTypes()[0];
		this.paramComponentType = TypeUtils.getComponentType(paramType, method.getGenericParameterTypes()[0]);
	}

	public void populateIntoObject(Entity ent, ObjectHolder dest) throws InstantiationException, IllegalAccessException
	{
		if (ent.hasProperty(name))
		{
			Object value = ent.getProperty(name);
			value = TypeUtils.convertFromDatastore(factory, value, paramType, paramComponentType);
			try
			{
				this.method.invoke(dest.get(), value);
			}
			catch (InvocationTargetException ex)
			{
				throw new RuntimeException(ex);
			}
		}
	}

	@Override
	public void populateFromList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		// should not be possible to reach here, you can't method populat
		// into an array
		throw new RuntimeException();
	}
}
