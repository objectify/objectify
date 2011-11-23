package com.googlecode.objectify.impl.load;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load things into a collection field.  Might be embedded items, might not.</p>
 */
public class CollectionLoader implements Loader
{
	/** */
	ObjectifyFactory fact;
	
	/** */
	Type fullType;
	Class<?> collectionType;
	
	/** */
	Loader componentLoader;
	
	/**
	 * @param type must be a Collection of some kind
	 */
	public CollectionLoader(ObjectifyFactory fact, Type type)
	{
		
		this.fullType = type;
		this.collectionType = GenericTypeReflector.erase(type);
		Type componentType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
		
		
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(com.googlecode.objectify.impl.load.EntityNode, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	public Object load(EntityNode node, LoadContext ctx)
	{
		@SuppressWarnings("unchecked")
		Collection<Object> collection = (Collection<Object>)fact.construct(collectionType);
		
		Iterator<Object> loaded = ctx.iterateLoad(node, componentLoader);
		while (loaded.hasNext()) {
			Object value = loaded.next();
			collection.add(value);
		}
		
		return collection;
	}
}
