package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Key;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.LoadConditions;
import com.googlecode.objectify.impl.Path;

import java.util.Collection;
import java.util.Map;

/**
 * The context of a save operation; might involve multiple entities (eg, batch save).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SaveContext
{
	/**
	 * Track all indexed values here. We may need to use some of this data to create synthetic
	 * indexes at the top level (ie, dot-separated indexes for v2 embedded saves).
	 */
	private final SetMultimap<Path, Object> indexes = HashMultimap.create();

	/**
	 * @param object can be either a single thing or an iterable list of things to index at this path
	 */
	public void addIndex(Path path, Object object) {
		if (object instanceof Iterable<?>)
			indexes.putAll(path, (Iterable<?>)object);
		else
			indexes.put(path, object);
	}
	
	/** */
	public Map<Path, Collection<Object>> getIndexes() {
		return indexes.asMap();
	}

	/**
	 * Subclass can ignore lifecycle methods.
	 */
	public boolean skipLifecycle() {
		return false;
	}

	/**
	 * Callback that we found a Ref in the object graph. Subclasses of this context may want to do something
	 * special with this.
	 */
	public Key saveRef(Ref<?> value, LoadConditions loadConditions) {
		return value.key().getRaw();
	}

	/**
	 * Called at the beginning of each entity save. In a batch save, this will be called more than once.
	 */
	public void startOneEntity() {
		indexes.clear();
	}
}