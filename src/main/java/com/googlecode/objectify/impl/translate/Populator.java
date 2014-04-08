package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.Path;

/**
 * <p>A populator knows how to copy properties between POJO objects and a PropertiesContainer.
 * Unlike a translator, it doesn't create the POJO or container.</p>
 *
 * <p>P is the pojo type.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Populator<P>
{
	/**
	 * <p>Loads the content of the specified datastore node, returning the pojo equivalent.</p>
	 *
	 * <p>There is one special value for loading: If a Result<?> is returned from a translator, the content of the
	 * Result will be used instead, but delayed until ctx.done() is called.  This happens at the end of a "round"
	 * of load operations and is the magic trick that makes populating entity references work efficiently.</p>
	 *
	 * @param node is the part of the native datastore entity tree we are transforming.
	 * @param ctx holds state information during an entity load.
	 * @param path is the current path to this translator
	 * @param into is an optional parameter; sometimes we need to create an object at a higher level and pass it
	 *             down through a stack of translators. Most translators will ignore this, but some will have special
	 *             behavior, for example, this allows collections to be recycled.
	 */
	void load(PropertyContainer node, LoadContext ctx, Path path, P into);

	/**
	 * Translates a pojo (or some component thereof) into a format suitable for storage in the datastore.
	 *
	 * @param pojo is an object from the pojo entity graph; possibly the whole graph or possibly just a leaf field.
	 * @param index is whether the instruction so far is to index or not index property values
	 * @param path is the path that we have taken to get here, which could be long due to re-entrant translators (ie,
	 *             an embedded pojo that also has a reference to the same class).
	 */
	void save(P pojo, boolean index, SaveContext ctx, Path path, PropertyContainer into);
}
