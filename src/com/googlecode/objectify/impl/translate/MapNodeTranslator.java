package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which expects a map node in the data structure and throws an exception if a map is not found.  Accepts
 * null values, just passing them on as a simple property.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class MapNodeTranslator<T> implements Translator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Translator#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(Node node, LoadContext ctx) {
		if (node.hasPropertyValue() && node.getPropertyValue() == null)
			return null;
		
		if (!node.hasMap())
			node.getPath().throwIllegalState("Expected map structure but found: " + node);
		
		return this.loadMap(node, ctx);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.Translator#save(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final public Node save(T pojo, Path path, boolean index, SaveContext ctx) {
		if (pojo == null) {
			Node node = new Node(path);
			node.setPropertyValue(null, index);
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
