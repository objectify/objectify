package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.node.EntityNode;

/**
 * <p>A translator knows how convert between POJO objects and the EntityNode tree structure
 * that Objectify can persist.  In principle, this is similar to how a JSON object mapper works.
 * Each node in the object graph corresponds to a node in the EntityNode graph.</p> 
 * 
 * <p>Translators are composed of other translators; through a chain of these a whole entity
 * object is assembled or disassembled.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Translator<T>
{
	/**
	 * Loads the content of the specified node, returning the pojo equivalent.
	 * 
	 * @param node is the part of the entity tree we are transforming.
	 * @param ctx holds state information during an entity load.  
	 * @return an assembled pojo corresponding to the node subtree
	 */
	T load(EntityNode node, LoadContext ctx);
	
	/**
	 * Translates the pojo into an EntityNode format
	 * 
	 * @param pojo is an object from the pojo entity graph; possibly the whole graph
	 * @param index is whether the instruction so far is to index or not index property values
	 * @return an EntityNode relevant to the pojo
	 */
	EntityNode save(T pojo, boolean index, SaveContext ctx); 
}
