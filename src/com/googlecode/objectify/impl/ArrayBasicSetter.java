package com.googlecode.objectify.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>BasicSetter which deals witha arrays of basic types.</p>
 */
public class ArrayBasicSetter extends BasicSetter
{
	/** @param field must be of array type */
	public ArrayBasicSetter(ObjectifyFactory fact, Field field)
	{
		super(fact, field);
		
		assert field.getType().isArray();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.BasicSetter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void set(Object obj, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + this.field);

		Collection<?> collValue = (Collection<?>)value;

		Class<?> componentType = this.field.getType().getComponentType();
		
		Object array = Array.newInstance(componentType, collValue.size());

		int index = 0;
		for (Object componentValue: collValue)
		{
			componentValue = this.importBasic(componentValue, componentType);
			Array.set(array, index++, componentValue);
		}

		TypeUtils.field_set(this.field, obj, array);
	}
}
