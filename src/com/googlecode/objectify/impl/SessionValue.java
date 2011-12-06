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
	public static class PartialProperty {
		Object pojo;
		Property property;
		com.google.appengine.api.datastore.Key key;
		
		public PartialProperty(Object pojo, Property prop, com.google.appengine.api.datastore.Key key) {
			this.pojo = pojo;
			this.property = prop;
			this.key = key;
		}
		
		public Object getPojo() { return this.pojo; }
		public Property getProperty() { return this.property; }
		public com.google.appengine.api.datastore.Key getKey() { return this.key; }
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
	List<PartialProperty> partials = new LinkedList<PartialProperty>();
	
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
	 * Set the stored result
	 */
	public void setResult(Result<T> value) {
		this.result = value;
	}
	
	/**
	 * Our best effort at making a meaningful string for debugging.
	 */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + key + ")";
	}

	/**
	 * As translation is occurring, some fields are set with partial entities.  These fields might
	 * need to be loaded with real entities during a subsequent fetch with different load groups.
	 * Every time a partial is filled, it is registered in the session value associated with the
	 * master entity... and if that master entity is reloaded with new load groups, the partials
	 * are checked to see if anything should be reloaded.
	 */
	public List<PartialProperty> getPartialProperties() {
		return this.partials;
	}
}
