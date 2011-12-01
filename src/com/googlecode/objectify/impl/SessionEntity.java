package com.googlecode.objectify.impl;

import java.util.LinkedHashSet;
import java.util.Set;

import com.googlecode.objectify.Result;


/**
 * The data we maintain in the session on behalf of an entity.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionEntity
{
	/** The entity value (possibly async) */
	Result<?> result;
	
	/**
	 * Groups that have been fetched for this entity.  If null, it means that all groups have been fetched.
	 */
	Set<String> groups;
	
	/**
	 * Somehow we need to track relationship fields for object graph walks
	 */
	LinkedHashSet<Object> relationships;
	
	public SessionEntity(Result<?> result) {
		this.result = result;
	}
	
	/**
	 * 
	 */
	@SuppressWarnings("unchecked")
	public <T> Result<T> getResult() { return (Result<T>)this.result; }
}
