package com.googlecode.objectify.impl;

import java.util.LinkedList;
import java.util.List;

import com.googlecode.objectify.Result;

/**
 * The information we maintain on behalf of an entity instance in the session cache.  Normally
 * this would just be a Result<?>, but we also need a list of upgrades so that future loads
 * with groups will patch up further references.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue<T>
{
	/** */
	Result<T> result;
	public Result<T> getResult() { return result; }

	/** Any remaining references that might need upgrading */
	final List<Upgrade> upgrades = new LinkedList<Upgrade>();
	public List<Upgrade> getUpgrades() { return upgrades; }

	/** */
	public SessionValue(Result<T> result) {
		this.result = result;
	}

	/** */
	public SessionValue(Result<T> result, List<Upgrade> upgrades) {
		this(result);
		this.upgrades.addAll(upgrades);
	}

	/** */
	public void addUpgrade(Upgrade upgrade) {
		upgrades.add(upgrade);
	}
}
