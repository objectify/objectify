package com.googlecode.objectify.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.engine.LoadBatch;


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
	abstract public static class Upgrade<U> {
		private Key<U> key;
		
		protected Property property;
		
		/** After prepare(), we will have one of these; on doUpgrade the result will be populated */
		protected Result<U> result;
		
		/** */
		public Upgrade(Property prop, Key<U> key) {
			this.property = prop;
			this.key = key;
		}

		/** */
		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(property=" + property.getName() + ", key=" + key + ")";
		}
		
		/** @return true if the upgrade should happen given the specified load groups */
		public boolean shouldLoad(Set<String> groups) {
			return property.shouldLoad(groups);
		}
		
		/**
		 * Starts the async result fetch; when doUpgrade is called later the result should be set on the relevant field.
		 */
		public void prepare(LoadBatch batch) {
			this.result = batch.getResult(key);
		}
		
		/**
		 * @return true if this upgrade has been prepared
		 */
		public boolean isPrepared() {
			return this.result != null;
		}
		
		abstract public void doUpgrade();
	}
	
	/**
	 * Key associated with the result.  Mostly here for debugging purposes.
	 */
	Key<T> key;
	
	/**
	 * The entity value wrapped in some number of layers of async.  It's possible this chain of layers might grow as
	 * additional groups are loaded; each one wraps the previous with another result that checks for upgrades.
	 */
	Result<T> result;
	
	/**
	 * List of groups that have been loaded for this value already.  As this list expands, the list of upgrades
	 * will shrink.  Note that this starts out null; that indicates that nothing has been loaded.  We will see
	 * this when an entity is put(); at this point no @Load fields are considered loaded, not even ones without
	 * explicit load groups.  When the field is an empty set, that indicates a load happened without any load groups.
	 */
	Set<String> loaded;
	
	/** 
	 * Track all the fields which might be upgraded in a future request which specifies additional load groups.
	 * This ends up being a list of all unfulfilled partial entity fields on this root entity (or its embedded
	 * objects). 
	 */
	List<Upgrade<?>> upgrades = new LinkedList<Upgrade<?>>();
	
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
	public List<Upgrade<?>> getUpgrades() {
		return this.upgrades;
	}
	
	/**
	 * Flag some groups as loaded.  Might be empty to indicate that a base load happened.
	 */
	public void addLoaded(Set<String> groups) {
		if (this.loaded == null)
			this.loaded = new HashSet<String>();
		
		this.loaded.addAll(groups);
	}
	
	/** @return true if all the specified groups have been loaded on this sessionvalue */
	public boolean isLoaded(Set<String> groups) {
		if (this.loaded == null)
			return false;
		
		for (String group: groups)
			if (!loaded.contains(group))
				return false;
		
		return true;
	}
}
