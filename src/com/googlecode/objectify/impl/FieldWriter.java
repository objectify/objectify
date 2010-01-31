package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;

/**
 */
class FieldWriter implements Writer
{
	private final ObjectifyFactory factory;
	private final Field field;
	private final String name;
	private final boolean listProperty;
	private final boolean indexed;

	FieldWriter(ObjectifyFactory factory, Field field, String name, boolean listProperty, boolean indexed)
	{
		this.factory = factory;
		this.field = field;
		this.name = name;
		this.listProperty = listProperty;
		this.indexed = indexed;
	}

	public void addtoEntity(Entity ent, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		value = convertToDatastore(value);
		if (value == null && listProperty)
		{
			return; // rule: omit when null or empty
		}

		if (!indexed)
			ent.setUnindexedProperty(name, value);
		else
			ent.setProperty(name, value);
		// TODO: Add warning if the field is indexed but we have converted it to Text (which is always unindexed).
	}

	public void addtoArray(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		value = convertToDatastore(value);
		entity.setValue(name, i, value);
	}

	public void initArray(ListPropertyMap arrays)
	{
		arrays.createArray(name, indexed);
	}

	/**
	 * Converts the value into an object suitable for storing in the datastore.
	 */
	public Object convertToDatastore(Object value)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof String)
		{
			// Check to see if it's too long and needs to be Text instead
			if (((String) value).length() > 500)
				return new Text((String) value);
		}
		else if (value instanceof Enum<?>)
		{
			return value.toString();
		}
		else if (value.getClass().isArray())
		{
			// The datastore cannot persist arrays, but it can persist ArrayList
			int length = Array.getLength(value);
			if (length == 0)
			{
				return null; // rule: omit when empty
			}
			ArrayList<Object> list = new ArrayList<Object>(length);

			for (int i = 0; i < length; i++)
				list.add(convertToDatastore(Array.get(value, i)));

			return list;
		}
		else if (value instanceof Collection<?>)
		{
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty())
			{
				return null; // rule: omit when empty
			}
			// All collections get turned into a List that preserves the order.  We must
			// also be sure to convert anything contained in the collection
			ArrayList<Object> list = new ArrayList<Object>(collection.size());

			for (Object obj : collection)
				list.add(convertToDatastore(obj));

			return list;
		}
		else if (value instanceof Key<?>)
		{
			return factory.oKeyToRawKey((Key<?>) value);
		}

		// Usually we just want to return the value
		return value;
	}
}
