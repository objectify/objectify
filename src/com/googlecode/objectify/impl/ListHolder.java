package com.googlecode.objectify.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * A lazily created list of objects (of a given type). The elements of the list will never be null.
 *
 * Clients can determine what the size of the list should be,
 * but if two clients think the size should be different, and exception is raised.
 */
public class ListHolder
{
	private final Class<?> type;
	private List<Object> list;

	public ListHolder(Class<?> type)
	{
		this.type = type;
	}

	public Object toArray(Class<?> componentType)
	{
		if (list == null) {
			return Array.newInstance(componentType, 0);
		}
		return list.toArray((Object[]) Array.newInstance(componentType, list.size()));
	}

	public int size()
	{
		return list == null ? 0 : list.size();
	}

	/**
	 * Initialize the underlying list to a specific size.
	 * An exception is thrown if the list has already been initialized to a different size
	 */
	public void initToSize(int size) throws IllegalAccessException, InstantiationException
	{
		if (list != null) {
			if (list.size() != size) {
				throw new RuntimeException("arrays didn't match"); // TODO is this how we want to handle this?
			}
			return;
		}
		// init it first time
		list = new ArrayList<Object>(size);
		for (int i = 0; i < size; i++)
		{
			list.add(type.newInstance());
		}
	}

	/**
	 * Get the ith element of the list.
	 * Callers should consider calling initToSize() to ensure the list is of the expected size.
	 */
	public Object get(int i)
	{
		return list == null ? null : list.get(i);
	}
}
