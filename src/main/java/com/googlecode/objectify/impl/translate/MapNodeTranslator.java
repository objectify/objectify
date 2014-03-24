package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which expects a map node in the data structure and throws an exception if a map is not found.  Accepts
 * null values, just passing them on as a simple property.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class MapNodeTranslator<T> extends AbstractTranslator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.AbstractTranslator#loadAbstract(com.googlecode.objectify.impl.Node, com.googlecode.objectify.impl.translate.LoadContext)
	 */
	@Override
	final public T loadAbstract(Node node, LoadContext ctx) {
		if (node.hasPropertyValue() && node.getPropertyValue() == null)
			return null;
		
		if (!node.hasMap())
			node.getPath().throwIllegalState("Expected map structure but found: " + node);
		
		return this.loadMap(node, ctx);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.AbstractTranslator#saveAbstract(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final public Node saveAbstract(T pojo, Path path, boolean index, SaveContext ctx) {
		if (pojo == null) {
			Node node = new Node(path);
			node.setPropertyValue(null, index);
			
			if (index)
				ctx.addIndex(path, null);
			
			return node;
		} else {
			return this.saveMap(pojo, path, index, ctx);
		}
	}
	
	/**
	 * Implement this knowing that we have a proper map node
	 */
	abstract protected T loadMap(Node node, LoadContext ctx);
	
	/**
	 * Implement this, returning a map node
	 */
	abstract protected Node saveMap(T pojo, Path path, boolean index, SaveContext ctx);
}
