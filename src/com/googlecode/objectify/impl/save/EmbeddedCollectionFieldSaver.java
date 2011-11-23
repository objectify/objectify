package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.ConverterRegistry;

/**
 * <p>Knows how to save an embedded collection.</p>
 *
 * @see EmbeddedMultivalueFieldSaver
 */
public class EmbeddedCollectionFieldSaver extends EmbeddedMultivalueFieldSaver
{
	/**
	 * @see EmbeddedMultivalueFieldSaver#EmbeddedMultivalueFieldSaver(ConverterRegistry, Class, Field, boolean, boolean)
	 */
	public EmbeddedCollectionFieldSaver(ObjectifyFactory fact, Class<?> examinedClass, Field field, boolean ignoreClassIndexing)
	{
		super(fact, examinedClass, field, ignoreClassIndexing);
		
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
