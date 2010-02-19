package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Knows how to save an embedded collection.</p>
 *
 * @see EmbeddedMultivalueFieldSaver
 */
public class EmbeddedCollectionFieldSaver extends EmbeddedMultivalueFieldSaver
{
	/**
	 * @see EmbeddedMultivalueFieldSaver#EmbeddedMultivalueFieldSaver(ObjectifyFactory, String, Field, boolean, boolean)
	 */
	public EmbeddedCollectionFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean inheritedIndexed, boolean collectionize)
	{
		super(fact, pathPrefix, field, inheritedIndexed, collectionize);
		
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
