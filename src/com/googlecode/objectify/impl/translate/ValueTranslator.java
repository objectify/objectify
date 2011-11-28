package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.MapNode;

/**
 * <p>Helper which helps take a mapnode's property value and converts it from datastore representation to pojo representation.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueTranslator<P, D> extends MapNodeTranslator<P>
{
	/** */
	protected Path path;
	
	/** */
	Class<D> datastoreClass;
	
	/** */
	public ValueTranslator(Path path, Class<D> datastoreClass) {
		this.path = path;
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
	 * @see com.googlecode.objectify.impl.translate.MapNodeTranslator#saveMap(java.lang.Object, com.googlecode.objectify.impl.Path, boolean, com.googlecode.objectify.impl.translate.SaveContext)
	 */
	@Override
	final protected MapNode saveMap(P pojo, Path path, boolean index, SaveContext ctx) {
		MapNode node = new MapNode(path);
		
		D translated = (pojo == null)
				? null
				: saveValue(pojo, ctx);
		
		// A quick sanity check - some things we cannot index!
		if (index && (translated instanceof Blob || translated instanceof Text))
			path.throwIllegalState("Request to index a value that cannot be indexed: " + translated);
		
		node.setPropertyValue(translated, index);
		
		return node;
	};
	
	/**
	 * Decode from a property value as stored in the datastore to a type that will be stored in a pojo.
	 * @param value will not be null, that has already been tested for
	 * @return the format which should be stored in the pojo; a null means store a literal null!
	 * @throws SkipException if this field subtree should be skipped
	 */
	abstract protected P loadValue(D value, LoadContext ctx) throws SkipException;
	
	/**
	 * Encode from a normal pojo value to a format that the datastore understands.  Note that a null return value
	 * is a literal instruction to store a null.
	 * 
	 * @param value will not be null, that has already been tested for
	 * @return the format which should be stored in the datastore; null means actually store a null!
	 * @throws SkipException if this subtree should be skipped
	 */
	abstract protected D saveValue(P value, SaveContext ctx) throws SkipException;
}
