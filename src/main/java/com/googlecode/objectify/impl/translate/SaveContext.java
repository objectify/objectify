package com.googlecode.objectify.impl.translate;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.googlecode.objectify.Objectify;
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
	/** The objectify instance */
	Objectify ofy;

	/** The current root entity; will change as multiple entities are loaded */
	Object currentRoot;

	/**
	 * Track all indexed values here. We may need to use some of this data to create synthetic
	 * indexes at the top level (ie, dot-separated indexes for v2 embedded saves).
	 */
	SetMultimap<Path, Object> indexes = HashMultimap.create();

	/** */
	public SaveContext(Objectify ofy) {
		this.ofy = ofy;
	}

	/** */
	public Objectify getObjectify() { return this.ofy; }

	/** Sets the current root entity, not its key! */
	public void setCurrentRoot(Object rootEntity) {
		this.currentRoot = rootEntity;
	}

	/** */
	public void addIndex(Path path, Object object) {
		indexes.put(path, object);
	}
	
	/** */
	public Map<Path, Collection<Object>> getIndexes() {
		return indexes.asMap();
	}
}