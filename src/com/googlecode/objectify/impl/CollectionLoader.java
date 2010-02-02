package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Embedded;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>A loader that knows how to persist collections of basic types.  This is not
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
public class CollectionLoader<T> extends Loader<T>
{
	/** */
	Field field;
	
	/** */
	public CollectionLoader(ObjectifyFactory fact, Navigator<T> nav, Field field)
	{
		super(fact, nav);
		
		assert (Collection.class.isAssignableFrom(field.getType()));
		assert (!field.getType().isArray());
		assert (!field.isAnnotationPresent(Embedded.class));

		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Loader#load(java.lang.Object, java.lang.Object)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void load(Object intoTarget, Object value)
	{
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + this.field);

		Collection<?> collValue = (Collection<?>)value;
		Collection<Object> target = (Collection<Object>)TypeUtils.field_get(this.field, intoTarget);

		if (target == null)
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
		
		for (Object obj: collValue)
			target.add(TypeUtils.convertFromDatastore(factory, obj, componentType));

		TypeUtils.field_set(this.field, intoTarget, target);
	}
}
