package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which expects a MapNode in the data structure and throws an exception if a ListNode is found.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class MapNodeTranslator<T> extends AbstractTranslator<T>
{
	/** */
	public MapNodeTranslator(Path path) {
		super(path);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Translator#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(EntityNode node, LoadContext ctx) {
		if (!(node instanceof MapNode))
			path.throwIllegalState("Found unexpected list structure: " + node);
		
		return this.loadMap((MapNode)node, ctx);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Translator#save(java.lang.Object, boolean, com.googlecode.objectify.impl.SaveContext)
	 */
	@Override
	final public EntityNode save(T pojo, boolean index, SaveContext ctx) {
		return this.saveMap(pojo, index, ctx);
	}
	
	/**
	 * Implement this knowing that we have a proper MapNode
	 */
	abstract protected T loadMap(MapNode node, LoadContext ctx);
	
	/**
	 * Implement this, returning a MapNode
	 */
	abstract public MapNode saveMap(T pojo, boolean index, SaveContext ctx);
}
