package com.googlecode.objectify.impl.load;

import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which helps take a mapnode's property value and converts it from datastore representation to pojo representation.</p>
 */
abstract public class ValueTranslator<P, D> extends MapNodeTranslator<P>
{
	/** */
	Class<D> datastoreClass;
	
	/** */
	public ValueTranslator(Path path, Class<D> datastoreClass) {
		super(path);
		this.datastoreClass = datastoreClass;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.MapNodeTranslator#loadMap(com.googlecode.objectify.impl.node.MapNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	final protected P loadMap(MapNode node, LoadContext ctx) {
		Object value = node.getPropertyValue();
		if (value == null)
			return null;
		
		if (!datastoreClass.isAssignableFrom(value.getClass()))
			path.throwIllegalState("Expected " + datastoreClass + ", got " + value.getClass() + ": " + value);
		
		@SuppressWarnings("unchecked")
		D d = (D)value;
		
		return loadValue(d, ctx);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.MapNodeTranslator#saveMap(java.lang.Object, boolean, com.googlecode.objectify.impl.SaveContext)
	 */
	@Override
	final public MapNode saveMap(P pojo, boolean index, SaveContext ctx) {
		MapNode node = new MapNode(path);
		D translated = saveValue(pojo, ctx);
		node.setPropertyValue(translated, index);
		
		return node;
	};
	
	/**
	 * Decode from a property value as stored in the datastore to a type that will be stored in a pojo.
	 * @param value will not be null
	 * @return the format which should be stored in the pojo
	 */
	abstract protected P loadValue(D value, LoadContext ctx);
	
	/**
	 * Encode from a normal pojo value to a format that the datastore understands
	 * @param value will not be null
	 * @return the format which should be stored in the pojo
	 */
	abstract protected D saveValue(P value, SaveContext ctx);
}
