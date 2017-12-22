package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.impl.Path;

/**
 * <p>A populator knows how to copy properties between POJO objects and the native datastore representation.
 * Unlike a translator, it doesn't create the POJO or container.</p>
 *
 * <p>P is the pojo type.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Populator<P>
{
	/**
	 * <p>Loads the content of the specified datastore node into an existing POJO.</p>
	 *
	 * @param node is the part of the native datastore entity tree we are transforming.
	 * @param ctx holds state information during an entity load.
	 * @param path is the current path to this POJO
	 */
	void load(FullEntity<?> node, LoadContext ctx, Path path, P into);

	/**
	 * Saves data from the POJO into the entity builder.
	 *
	 * @param pojo is an object from the pojo entity graph; possibly the whole graph or possibly just a leaf field.
	 * @param index is whether the instruction so far is to index or not index property values
	 * @param path is the path that we have taken to get here, which could be long due to re-entrant translators (ie,
	 *             an embedded pojo that also has a reference to the same class).
	 */
	void save(P pojo, boolean index, SaveContext ctx, Path path, FullEntity.Builder<?> into);
}
