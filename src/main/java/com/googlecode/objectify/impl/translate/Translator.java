package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.LoadEngine;
import com.googlecode.objectify.impl.Path;

/**
 * <p>A translator knows how convert between POJO objects and the native datastore representation.</p>
 *
 * <p>Translators are composed of other translators; through a chain of these a whole entity
 * object is assembled or disassembled.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Translator<T>
{
	/**
	 * <p>Loads the content of the specified datastore node, returning the pojo equivalent.</p>
	 * 
	 * <p>There is one special return value: If a Result<?> is returned, the content of the Result will be used instead,
	 * but delayed until ctx.done() is called.  This happens at the end of a "round" of load operations and is
	 * the magic trick that makes populating entity references work efficiently.</p>
	 * 
	 * @param node is the part of the native datastore entity tree we are transforming.
	 * @param ctx holds state information during an entity load.  
	 * @return an assembled pojo corresponding to the node subtree; if null is returned, that is the real value!
	 * 
	 * @throws SkipException if the subtree should not be loaded into a containing entity
	 * 
	 * @see LoadEngine
	 */
	T load(Object node, LoadContext ctx) throws SkipException;

	/**
	 * Translates a pojo (or some component thereof) into a format suitable for storage in the datastore.
	 *
	 * @param pojo is an object from the pojo entity graph; possibly the whole graph or possibly just a leaf field.
	 * @param index is whether the instruction so far is to index or not index property values
	 * @param path is the path that we have taken to get here, which could be long due to re-entrant translators (ie,
	 *             an embedded pojo that also has a reference to the same class).
	 * @return something suitable for storage in the datastore.
	 * 
	 * @throws SkipException if this subtree should not be saved.
	 */
	Object save(T pojo, boolean index, SaveContext ctx, Path path) throws SkipException;
}
