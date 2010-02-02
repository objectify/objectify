package com.googlecode.objectify.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.Embedded;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * A loader that knows how to persist an array of basic types.  This is not
 * to be used with arrays of embedded objects, which aren't "real" arrays that
 * can be stored in the datastore.
 */
public class ArrayLoader<T> extends Loader<T>
{
	/** */
	Field field;
	
	/** */
	public ArrayLoader(ObjectifyFactory fact, Navigator<T> nav, Field field)
	{
		super(fact, nav);
		
		assert (field.getType().isArray());
		assert (!field.isAnnotationPresent(Embedded.class));
		assert (!Collection.class.isAssignableFrom(field.getType()));

		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Loader#load(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void load(Object intoTarget, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + this.field);

		Collection<?> collValue = (Collection<?>)value;

		Class<?> componentType = this.field.getType().getComponentType();
		
		Object array = Array.newInstance(componentType, collValue.size());

		int index = 0;
		for (Object componentValue: collValue)
		{
			componentValue = TypeUtils.convertFromDatastore(factory, componentValue, componentType);
			Array.set(array, index++, componentValue);
		}

		TypeUtils.field_set(this.field, intoTarget, array);
	}
}
