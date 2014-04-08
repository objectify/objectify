package com.googlecode.objectify.impl;

import com.googlecode.objectify.Result;

import java.util.HashSet;
import java.util.Set;

/**
 * The information we maintain on behalf of an entity instance in the session cache.  Normally
 * this would just be a Result<?>, but we also need a list of references so that future loads
 * with groups will patch up further references.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue<T>
{
	/** */
	Result<T> result;
	public Result<T> getResult() { return result; }

	/**
	 * Tracks the load groups which have been active when this session value was traversed during a reload
	 * operation. If this value has already been loaded with LoadGroupA, then
	 * we don't need to traverse the references again when LoadGroupA is active. Importantly, this
	 * prevents cycles within a single load operation when there are cycles in the object graph.
	 */
	Set<Class<?>> loadedGroups = new HashSet<Class<?>>();

	/** */
	public SessionValue(Result<T> result) {
		this.result = result;
	}

	/** @return false if loadgroup has already been added */
	public boolean addLoadGroup(Class<?> loadGroup) {
		return loadedGroups.add(loadGroup);
	}
}
