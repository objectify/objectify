package com.googlecode.objectify.impl;

/**
 * A thin holder of an Object, which is instantiated the first time get() is called.
 * A reference to a lazily-created object.
 */
public class ObjectHolder
{
	private final Class<?> type;
	private Object object;

	public ObjectHolder(Class<?> type)
	{
		this.type = type;
	}

	public ObjectHolder(Object obj)
	{
		this.type = obj.getClass();
		this.object = obj;
	}

	public Object get() throws IllegalAccessException, InstantiationException
	{
		if (object == null)
		{
			object = type.newInstance();
		}
		return object;
	}

	public boolean wasCreated()
	{
		return object != null;
	}
}
