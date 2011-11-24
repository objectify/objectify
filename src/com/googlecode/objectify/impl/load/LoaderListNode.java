package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which expects a ListNode in the data structure and throws an exception if a MapNode is found.</p>
 */
abstract public class LoaderListNode<T> extends LoaderAbstract<T>
{
	/** */
	public LoaderListNode(Path path) {
		super(path);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(EntityNode node, LoadContext ctx) {
		if (!(node instanceof MapNode))
			path.throwIllegalState("Expected a list structure but found " + node);
		
		return this.load((ListNode)node, ctx);
	}
	
	/**
	 * Implement this knowing that we have a proper ListNode
	 */
	abstract protected T load(ListNode node, LoadContext ctx);
}
