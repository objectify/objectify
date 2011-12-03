package com.googlecode.objectify.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;


/**
 * The data we maintain in the session on behalf of an entity.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionEntity<T>
{
	/**
	 * Key associated with the result.  Mostly here for debugging purposes.
	 */
	Key<T> key;
	
	/**
	 * The entity value (possibly async)
	 */
	Result<T> result;
	
	/**
	 * Groups that have been fetched for this entity.  If null, it means that all groups have been fetched.
	 */
	Set<String> groups;
	
	/**
	 * Somehow we need to track relationship fields for object graph walks
	 */
	LinkedHashSet<Object> relationships;
	
	/**
	 */
	public SessionEntity(Key<T> key, Result<T> result) {
		this.key = key;
		this.result = result;
	}
	
	/**
	 * Get the key permanently associated with this sessionentity
	 */
	public Key<T> getKey () {
		return this.key;
	}
	
	/**
	 * Get the stored result
	 */
	public Result<T> getResult() {
		return this.result;
	}
	
	/**
	 * Our best effort at making a meaningful string for debugging.
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + key + ")";
	}
}
