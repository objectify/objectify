package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which helps take a mapnode's property value and converts it from datastore representation to pojo representation.</p>
 */
abstract public class LoaderPropertyValue<P, D> extends LoaderMapNode<P>
{
	/** */
	Class<D> datastoreClass;
	
	/** */
	public LoaderPropertyValue(Path path, Class<D> datastoreClass) {
		super(path);
		this.datastoreClass = datastoreClass;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.LoaderMapNode#load(com.googlecode.objectify.impl.node.MapNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final protected P load(MapNode node, LoadContext ctx) {
		Object value = node.getPropertyValue();
		if (value == null)
			return null;
		
		if (!datastoreClass.isAssignableFrom(value.getClass()))
			path.throwIllegalState("Expected " + datastoreClass + ", got " + value.getClass() + ": " + value);
		
		@SuppressWarnings("unchecked")
		D d = (D)value;
		
		return load(d, ctx);
	}
	
	/**
	 * Convert from a property value as stored in the datastore to a type that will be stored in a pojo.
	 * @param propertyValue will not be null
	 * @return the format which should be stored in the pojo
	 */
	abstract protected P load(D propertyValue, LoadContext ctx);
}
