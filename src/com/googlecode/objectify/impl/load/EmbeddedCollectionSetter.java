package com.googlecode.objectify.impl.load;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Embedded;

import com.googlecode.objectify.impl.FieldWrapper;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>This setter handles embedded collections similar to embedded arrays.  The special
 * consideration of collections follows the documentation for {@code TypeUtils.prepareCollection()}.</p>
 * 
 * @see TypeUtils#prepareCollection(Object, com.googlecode.objectify.impl.Wrapper)
 */
public class EmbeddedCollectionSetter extends Setter
{
	/**
	 * The field which holds the embedded collection. We use FieldWrapper instead of
	 * Field because we want to use methods that take a the wrapper type.
	 */
	FieldWrapper field;
	Class<?> componentType;
	Constructor<?> componentTypeCtor;

	/** */
	public EmbeddedCollectionSetter(Field field)
	{
		assert field.isAnnotationPresent(Embedded.class);
		assert Collection.class.isAssignableFrom(field.getType());
		
		this.field = new FieldWrapper(field);
		this.componentType = TypeUtils.getComponentType(this.field.getType(), this.field.getGenericType());
		this.componentTypeCtor = TypeUtils.getNoArgConstructor(this.componentType);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	public void set(Object toPojo, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Tried to load a non-collection type into embedded collection " + this.field);

		Collection<?> datastoreCollection = (Collection<?>)value;
		Collection<Object> embeddedCollection = (Collection<Object>)TypeUtils.prepareCollection(toPojo, field);

		if (embeddedCollection.isEmpty())
		{
			// Initialize it with relevant POJOs
			for (int i=0; i<datastoreCollection.size(); i++)
			{
				Object embedded = TypeUtils.newInstance(componentTypeCtor);
				embeddedCollection.add(embedded);
			}
		}
		
		Iterator<Object> embeddedIt = embeddedCollection.iterator();
		for (Object datastoreValue: datastoreCollection)
		{
			Object embedded = embeddedIt.next();
			this.next.set(embedded, datastoreValue);
		}
	}
}
