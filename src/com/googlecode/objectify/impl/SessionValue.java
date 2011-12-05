package com.googlecode.objectify.impl;

import java.util.LinkedList;
import java.util.List;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;


/**
 * The data we maintain in the session on behalf of an entity.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue<T>
{
	/**
	 * We track each property in the entity that is unfetched so that subsequent loads which
	 * may have different load groups might populate the relevant fields. 
	 */
	public static class Unfetched {
		Property prop;
	}
	
	/**
	 * Key associated with the result.  Mostly here for debugging purposes.
	 */
	Key<T> key;
	
	/**
	 * The entity value (possibly async)
	 */
	Result<T> result;
	
	/** 
	 * Track all the fields which might be fetched in a different load group 
	 */
	List<Unfetched> unfetched = new LinkedList<Unfetched>();
	
	/**
	 */
	public SessionValue(Key<T> key, Result<T> result) {
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
