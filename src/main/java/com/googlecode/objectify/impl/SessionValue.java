package com.googlecode.objectify.impl;

import com.googlecode.objectify.Result;

import java.util.HashSet;
import java.util.Set;

/**
 * The information we maintain on behalf of an entity instance in the session cache.  Normally
 * this would just be a Result<?>, but we also need to track the load arrangements so that
 * we can decide whether to look for more Ref<?>s to load.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue<T>
{
	/** */
	Result<T> result;
	public Result<T> getResult() { return result; }

	/**
	 * <p>Keep track of every load group arrangement that has been seen so far. We know that if we see
	 * a new arrangement, we will need to save() the POJO to an entity (which gets tossed) so that
	 * we can look for any Ref<?>s and possibly load them with the new instructions.</p>
	 *
	 * <p>Also, this prevents cycles within a single load operation when there are cycles in the object graph.</p>
	 */
	Set<LoadArrangement> loadedWith = new HashSet<>();

	/**
	 * No load arrangement - in other words, this was a save operation
	 */
	public SessionValue(Result<T> result) {
		this.result = result;
	}

	/** */
	public SessionValue(Result<T> result, LoadArrangement loadArrangement) {
		this(result);
		this.loadedWith.add(loadArrangement);
	}

	/**
	 * @return false if the arrangement has already been added
	 */
	public boolean loadWith(LoadArrangement arrangement) {
		return loadedWith.add(arrangement);
	}
}
