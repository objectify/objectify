package com.googlecode.objectify.impl;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.Embedded;

/**
 * <p>This setter handles an embedded array by constructing the array and all the objects
 * inside.  It expects the value to be a collection type so it "fans out" the values to
 * all the embedded objects in the array, calling the next setter in the chain with each
 * value.</p>
 */
public class EmbeddedCollectionSetter extends Setter
{
	/** The field which holds the embedded array */
	Field field;
	
	/** */
	public EmbeddedCollectionSetter(Field field)
	{
		assert field.isAnnotationPresent(Embedded.class);
		assert field.getType().isArray();
		
		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	public void set(Object obj, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Tried to load a non-collection type into embedded array " + this.field);
		
		Collection<?> collValue = (Collection<?>)value;

		// First we need the actual array of child objects
		Object array = TypeUtils.field_get(this.field, obj);
		if (array == null)
		{
			Class<?> componentType = this.field.getType().getComponentType();
			array = Array.newInstance(componentType, collValue.size());

			int index = 0;
			for (Object componentValue: collValue)
			{
				componentValue = TypeUtils.class_newInstance(componentType);
				Array.set(array, index++, componentValue);
			}
		}

		// Now we can fan out the sets on the embedded classes
		int index = 0;
		for (Object embedded: collValue)
			this.next.set(Array.get(array, index++), embedded);
	}
}
