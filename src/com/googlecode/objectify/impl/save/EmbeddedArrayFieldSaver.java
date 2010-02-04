package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Iterator;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>Knows how to save an embedded array.</p>
 *
 * @see EmbeddedIteratorFieldSaver
 */
public class EmbeddedArrayFieldSaver extends EmbeddedIteratorFieldSaver
{
	/**
	 * @see EmbeddedIteratorFieldSaver#EmbeddedIteratorFieldSaver(ObjectifyFactory, String, Field, boolean, boolean)
	 */
	public EmbeddedArrayFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean forceUnindexed, boolean collectionize)
	{
		super(fact, pathPrefix, field, forceUnindexed, collectionize);
		
		assert field.getType().isArray();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#getComponentType()
	 */
	@Override
	protected Class<?> getComponentType()
	{
		return this.field.getType().getComponentType();
	}


	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#iterator(java.lang.Object)
	 */
	@Override
	protected Iterator<Object> iterator(Object arrayOrCollection)
	{
		return Arrays.asList((Object[])arrayOrCollection).iterator();
	}
}
