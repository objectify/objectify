package com.googlecode.objectify.impl;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.IgnoreLoad;
import com.googlecode.objectify.annotation.Load;

import java.lang.annotation.Annotation;
import java.util.LinkedHashSet;
import java.util.Set;

/** 
 * Interface for something that constructs objects. This is used to abstract the injection/construction
 * process so we don't have to pass ObjectifyFactory around everywhere.
 */
public interface Forge
{
	/**
	 * <p>Construct an instance of the specified type.  Objectify uses this method whenever possible to create
	 * instances of entities, condition classes, or other types.</p>
	 */
	<T> T construct(Class<T> type);
}