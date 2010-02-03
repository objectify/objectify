package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>BasicSetter which deals with collections of basic types. This is not
 * to be used with collections of embedded objects, which aren't "real" collections that
 * can be stored in the datastore.</p>
 * 
 * <p>There are a number of special considerations for collections:</p>
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
public class CollectionBasicSetter extends BasicSetter
{
	/** @param field must be of array type */
	public CollectionBasicSetter(ObjectifyFactory fact, Field field)
	{
		super(fact, field);
		
		assert Collection.class.isAssignableFrom(field.getType());
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.BasicSetter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void set(Object obj, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + this.field);

		Collection<?> collValue = (Collection<?>)value;
		Collection<Object> target = (Collection<Object>)TypeUtils.field_get(this.field, obj);

		if (target != null)
		{
			target.clear();
		}
		else
		{
			if (!this.field.getType().isInterface())
			{
				target = (Collection<Object>)TypeUtils.class_newInstance(this.field.getType());
			}
			else if (SortedSet.class.isAssignableFrom(this.field.getType()))
			{
				target = new TreeSet<Object>();
			}
			else if (Set.class.isAssignableFrom(this.field.getType()))
			{
				target = new HashSet<Object>();
			}
			else if (List.class.isAssignableFrom(this.field.getType()) || this.field.getType().isAssignableFrom(ArrayList.class))
			{
				target = new ArrayList<Object>();
			}
		}

		Class<?> componentType = TypeUtils.getComponentType(this.field.getType(), this.field.getGenericType());
		
		for (Object member: collValue)
			target.add(this.importBasic(member, componentType));

		TypeUtils.field_set(this.field, obj, target);
	}
}
