package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which expects a list node in the data structure and throws an exception one is not found.
 * Also handles skipping when a null list is found.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ListNodeTranslator<T> implements Translator<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.node.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final public T load(Node node, LoadContext ctx) {
		// Just ignore nulls for all collection types
		if (node.hasPropertyValue() && node.getPropertyValue() == null)
			throw new SkipException();
		
		if (!node.hasList())
			node.getPath().throwIllegalState("Expected list structure but found " + node);
		
		return this.loadList(node, ctx);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.Translator#save(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final public Node save(T pojo, Path path, boolean index, SaveContext ctx) {
		// If the collection is null, just skip it.  This is important because of the way filtering works;
		// if we stored a null then the field would match when filtering for null (same as a null in the list).
		// Also, storing a null would forcibly assign null to the collection field on load, screwing things up
		// if the developer decided to later initialize the collection in the default constructor.
		if (pojo == null)
			throw new SkipException();
		
		return this.saveList(pojo, path, index, ctx);
	};
	
	/**
	 * Implement this knowing that we have a proper list node
	 */
	abstract protected T loadList(Node node, LoadContext ctx);
	
	/**
	 * Implement this to return a proper list node
	 */
	abstract protected Node saveList(T pojo, Path path, boolean index, SaveContext ctx);
}
