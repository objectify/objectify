package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which expects a MapNode in the data structure and throws an exception if a ListNode is found.</p>
 */
abstract public class LoaderMapNode<T> extends LoaderAbstract<T>
{
	/** */
	public LoaderMapNode(Path path) {
		super(path);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(EntityNode node, LoadContext ctx) {
		if (!(node instanceof MapNode))
			path.throwIllegalState("Found unexpected list structure: " + node);
		
		return this.load((MapNode)node, ctx);
	}
	
	/**
	 * Implement this knowing that we have a proper MapNode
	 */
	abstract protected T load(MapNode node, LoadContext ctx);
}
