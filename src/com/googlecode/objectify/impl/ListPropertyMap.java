package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a map of property-name to List<Object>.
 * This information can then be written into an Entity.
 */
public class ListPropertyMap
{
	private static class Info
	{
		List<Object> array;
		boolean indexed;
	}

	private final int length;
	private final Map<String, Info> arrays = new HashMap<String, Info>();

	public ListPropertyMap(int length)
	{
		this.length = length;
	}

	public void createArray(String name, boolean indexed)
	{
		if (arrays.containsKey(name))
		{
			throw new IllegalStateException("Internal error, the same array property was created twice, '" + name + "'.");
		}
		Info i = new Info();
		i.array = new ArrayList<Object>(length);
		i.indexed = indexed;
		arrays.put(name, i);
	}

	public void addToEntity(Entity ent)
	{
		for (Map.Entry<String, Info> entry : arrays.entrySet())
		{
			String name = entry.getKey();
			List<?> value = entry.getValue().array;
			// arrays of length zero are effectively a nop, not omitted
			if (entry.getValue().indexed)
				ent.setProperty(name, value);
			else
				ent.setUnindexedProperty(name, value);
		}
	}

	public void setValue(String name, int i, Object value)
	{
		arrays.get(name).array.add(i, value);
	}
}
