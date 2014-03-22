package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which expects a property value in the data structure and throws an exception if one is not found.
 * Also handles null checking.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class PropertyValueNodeTranslator<T> extends AbstractTranslator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.AbstractTranslator#loadAbstract(com.googlecode.objectify.impl.Node, com.googlecode.objectify.impl.translate.LoadContext)
	 */
	@Override
	final public T loadAbstract(Node node, LoadContext ctx) {
		if (!node.hasPropertyValue())
			node.getPath().throwIllegalState("Expected property value but found: " + node);
		
		if (node.getPropertyValue() == null)
			return null;
		
		return this.loadPropertyValue(node, ctx);
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
			return this.savePropertyValue(pojo, path, index, ctx);
		}
	}
	
	/**
	 * Implement this knowing that we have a proper property value node with a non-null value
	 * @param node will have a property value and will never be null
	 */
	abstract protected T loadPropertyValue(Node node, LoadContext ctx);
	
	/**
	 * Implement this, returning a property value node.
	 * @param pojo will never be null
	 */
	abstract protected Node savePropertyValue(T pojo, Path path, boolean index, SaveContext ctx);
}
