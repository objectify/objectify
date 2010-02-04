package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Knows how to save an embedded collection.</p>
 *
 * @see EmbeddedIteratorFieldSaver
 */
public class EmbeddedCollectionFieldSaver extends EmbeddedIteratorFieldSaver
{
	/**
	 * @see EmbeddedIteratorFieldSaver#EmbeddedIteratorFieldSaver(ObjectifyFactory, String, Field, boolean, boolean)
	 */
	public EmbeddedCollectionFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean forceUnindexed, boolean collectionize)
	{
		super(fact, pathPrefix, field, forceUnindexed, collectionize);
		
		assert Collection.class.isAssignableFrom(field.getType());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#getComponentType()
	 */
	@Override
	protected Class<?> getComponentType()
	{
		return TypeUtils.getComponentType(this.field.getType(), this.field.getGenericType());
	}


	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#iterator(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Iterator<Object> iterator(Object arrayOrCollection)
	{
		return ((Collection<Object>)arrayOrCollection).iterator();
	}
}
