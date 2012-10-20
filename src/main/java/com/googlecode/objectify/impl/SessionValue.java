package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Load;

/**
 * The information we maintain on behalf of an entity instance in the session cache.  Normally
 * this would just be a Result<?>, but we also need a list of upgrades so that future loads
 * with groups will patch up further references.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue
{
	/** */
	Result<?> result;

	/** Any remaining references that might need upgrading */
	Map<Key<?>, Load> upgrades = new HashMap<Key<?>, Load>();
}
