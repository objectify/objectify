package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;


/**
 * The data we maintain in the session on behalf of an entity.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SessionValue
{
	/**
	 * Key associated with the result.  Mostly here for debugging purposes.
	 */
	Key<?> key;
	
	/**
	 * The entity value wrapped in some number of layers of async.
	 */
	Result<Entity> result;
	
	/**
	 */
	public SessionValue(Key<?> key, Result<Entity> result) {
		this.key = key;
		this.result = result;
	}
	
	/**
	 * Get the key permanently associated with this sessionentity
	 */
	public Key<?> getKey () {
		return this.key;
	}
	
	/**
	 * Get the stored result
	 */
	public Result<Entity> getResult() {
		return this.result;
	}
	
	/**
	 * Set the stored result
	 */
	public void setResult(Result<Entity> value) {
		this.result = value;
	}
	
	/**
	 * Our best effort at making a meaningful string for debugging.
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + key + ")";
	}
}
