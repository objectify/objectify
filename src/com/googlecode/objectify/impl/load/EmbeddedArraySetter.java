package com.googlecode.objectify.impl.load;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;

import javax.persistence.Embedded;

import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>This setter handles an embedded array by constructing the array and all the objects
 * inside.  It expects the value to be a collection type so it "fans out" the values to
 * all the embedded objects in the array, calling the next setter in the chain with each
 * value.</p>
 */
public class EmbeddedArraySetter extends Setter
{
	/** The field which holds the embedded array */
	Field field;
	Class<?> componentType;
	Constructor<?> componentTypeCtor;

	/** */
	public EmbeddedArraySetter(Field field)
	{
		assert field.isAnnotationPresent(Embedded.class);
		assert field.getType().isArray();
		
		this.field = field;
		this.componentType = this.field.getType().getComponentType();
		this.componentTypeCtor = TypeUtils.getNoArgConstructor(this.componentType);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void set(Object toPojo, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Tried to load a non-collection type into embedded array " + this.field);
		
		Collection<?> datastoreCollection = (Collection<?>)value;

		// First we need the actual array of child objects
		Object embeddedArray = TypeUtils.field_get(this.field, toPojo);
		if (embeddedArray == null)
		{
			// Make the array and set it on the pojo field
			embeddedArray = Array.newInstance(componentType, datastoreCollection.size());
			TypeUtils.field_set(this.field, toPojo, embeddedArray);

			// Populate the array with fresh embedded objects.
			for (int i=0; i<datastoreCollection.size(); i++)
			{
				Object embedded = TypeUtils.newInstance(componentTypeCtor);
				Array.set(embeddedArray, i, embedded);
			}
		}

		// Now we can fan out the set() calls on the embedded classes
		int index = 0;
		for (Object datastoreValue: datastoreCollection)
			this.next.set(Array.get(embeddedArray, index++), datastoreValue);
	}
}
