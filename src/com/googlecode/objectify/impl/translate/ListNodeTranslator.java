package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.EntityNode;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which expects a list node in the data structure and throws an exception one is not found.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ListNodeTranslator<T> implements Translator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(EntityNode node, LoadContext ctx) {
		if (!node.hasList())
			node.getPath().throwIllegalState("Expected list structure but found " + node);
		
		return this.loadList(node, ctx);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.Translator#save(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final public EntityNode save(T pojo, Path path, boolean index, SaveContext ctx) {
		return this.saveList(pojo, path, index, ctx);
	};
	
	/**
	 * Implement this knowing that we have a proper list node
	 */
	abstract protected T loadList(EntityNode node, LoadContext ctx);
	
	/**
	 * Implement this to return a proper list node
	 */
	abstract protected EntityNode saveList(T pojo, Path path, boolean index, SaveContext ctx);
}
