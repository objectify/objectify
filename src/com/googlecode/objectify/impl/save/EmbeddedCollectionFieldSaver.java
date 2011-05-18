package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Collection;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Conversions;

/**
 * <p>Knows how to save an embedded collection.</p>
 *
 * @see EmbeddedMultivalueFieldSaver
 */
public class EmbeddedCollectionFieldSaver extends EmbeddedMultivalueFieldSaver
{
	/**
	 * @see EmbeddedMultivalueFieldSaver#EmbeddedMultivalueFieldSaver(Conversions, Class, Field, boolean, boolean)
	 */
	public EmbeddedCollectionFieldSaver(Conversions conv, Class<?> examinedClass, Field field, boolean ignoreClassIndexing, boolean collectionize)
	{
		super(conv, examinedClass, field, ignoreClassIndexing, collectionize);
		
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
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#asCollection(java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected Collection<Object> asCollection(Object arrayOrCollection)
	{
		return (Collection<Object>)arrayOrCollection;
	}
}
