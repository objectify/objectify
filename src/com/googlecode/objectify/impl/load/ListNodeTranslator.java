package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which expects a ListNode in the data structure and throws an exception if a MapNode is found.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ListNodeTranslator<T> extends AbstractTranslator<T>
{
	/** */
	public ListNodeTranslator(Path path) {
		super(path);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(EntityNode node, LoadContext ctx) {
		if (!(node instanceof MapNode))
			path.throwIllegalState("Expected a list structure but found " + node);
		
		return this.loadList((ListNode)node, ctx);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Translator#save(java.lang.Object, boolean, com.googlecode.objectify.impl.SaveContext)
	 */
	@Override
	final public EntityNode save(T pojo, boolean index, SaveContext ctx) {
		return this.saveList(pojo, index, ctx);
	};
	
	/**
	 * Implement this knowing that we have a proper ListNode
	 */
	abstract protected T loadList(ListNode node, LoadContext ctx);
	
	/**
	 * Implement this to return a proper ListNode
	 */
	abstract protected ListNode saveList(T pojo, boolean index, SaveContext ctx);
}
