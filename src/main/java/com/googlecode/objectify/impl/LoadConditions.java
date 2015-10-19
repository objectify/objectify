package com.googlecode.objectify.impl;

import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;
import java.util.Set;

/** 
 * A 'brain' class that determines whether or not refs should be loaded.
 */
public class LoadConditions
{
	/** The states are important - null means none, empty means "all" */
	Class<?>[] loadGroups;

	/** This will never be empty - either null or have some values */
	Class<?>[] loadUnlessGroups;

	/** Whether or not this field is a @Parent field */
	boolean parent;

	/**
	 * @param load can be null; it's "whatever was specified on the field", possibly nothing
	 * @param parent will be null for nonparent fields
	 */
	public LoadConditions(Load load, Parent parent) {
		// Get @Load groups
		if (load != null) {
			loadGroups = load.value();
			
			if (load.unless().length > 0)
				loadUnlessGroups = load.unless();
		}

		this.parent = parent != null;
	}

	/**
	 * @return true if the property should be loaded when the given loadgroups are active
	 */
	public boolean shouldLoad(Set<Class<?>> groups, boolean inTransaction) {
		if (loadGroups == null)
			return false;
		
		if (loadGroups.length > 0 && !matches(groups, loadGroups))
			return false;

		if (loadUnlessGroups != null && matches(groups, loadUnlessGroups))
			return false;

		if (inTransaction) {
			if (parent) {
				return true;	// even in transactions, it's always safe to load the parent - same entity group
			} else {
				// Let explicitly specified load groups work in transactions, otherwise not at all - too dangerous
				return loadGroups.length > 0;
			}
		} else {
			return true;
		}
	}

	/**
	 */
	private boolean matches(Set<Class<?>> groups, Class<?>[] loadGroups) {
		for (Class<?> propertyGroup: loadGroups)
			for (Class<?> enabledGroup: groups)
				if (propertyGroup.isAssignableFrom(enabledGroup))
					return true;
		
		return false;
	}
}