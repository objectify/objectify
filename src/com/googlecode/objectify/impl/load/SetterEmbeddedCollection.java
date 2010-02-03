package com.googlecode.objectify.impl.load;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Iterator;

import javax.persistence.Embedded;

import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>This setter handles embedded collections similar to embedded arrays.  The special
 * consideration of collections follows the approach for collections of basic types:</p>
 * <ul>
 * <li>If the target field already contains a collection object, it will be cleared and
 * repopulated.  A new instance will not be created.</li>
 * <li>If the target field is a concrete collection type, an instance of the concrete type
 * will be created.</li>
 * <li>If the target field is Set, a HashSet will be created.</li>  
 * <li>If the target field is SortedSet, a TreeSet will be created.</li>  
 * <li>If the target field is List, an ArrayList will be created.</li>  
 * </ul>
 */
public class SetterEmbeddedCollection extends Setter
{
	/**
	 * The field which holds the embedded collection. We use FieldWrapper instead of
	 * Field because we want to use methods that take a the wrapper type.
	 */
	FieldWrapper field;
	
	/** */
	public SetterEmbeddedCollection(Field field)
	{
		assert field.isAnnotationPresent(Embedded.class);
		assert Collection.class.isAssignableFrom(field.getType());
		
		this.field = new FieldWrapper(field);
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

		Class<?> componentType = TypeUtils.getComponentType(field.getType(), field.getGenericType());
		
		if (embeddedCollection.isEmpty())
		{
			// Initialize it with relevant POJOs
			for (int i=0; i<datastoreCollection.size(); i++)
			{
				Object embedded = TypeUtils.class_newInstance(componentType);
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
