package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;

import com.googlecode.objectify.impl.conv.StandardConversions;

/**
 * <p>Knows how to save an embedded array.</p>
 *
 * @see EmbeddedMultivalueFieldSaver
 */
public class EmbeddedArrayFieldSaver extends EmbeddedMultivalueFieldSaver
{
	/**
	 * @see EmbeddedMultivalueFieldSaver#EmbeddedMultivalueFieldSaver(StandardConversions, Class, Field, boolean, boolean)
	 */
	public EmbeddedArrayFieldSaver(StandardConversions conv, Class<?> examinedClass, Field field, boolean ignoreClassIndexing, boolean collectionize)
	{
		super(conv, examinedClass, field, ignoreClassIndexing, collectionize);
		
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
	 * @see com.googlecode.objectify.impl.save.EmbeddedIteratorFieldSaver#asCollection(java.lang.Object)
	 */
	@Override
	protected Collection<Object> asCollection(Object arrayOrCollection)
	{
		return Arrays.asList((Object[])arrayOrCollection);
	}
}
