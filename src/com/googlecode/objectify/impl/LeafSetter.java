package com.googlecode.objectify.impl;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>Setter which knows how to set any kind of leaf value.  This could be any basic type
 * or a collection of basic types; basically anything except an @Embedded.</p>
 * 
 * <p>This class is a little weird.  It is a termination setter which itself cannot be
 * extended.  However, when constructed, it actually sets its next value to be a more-specific
 * type of setter defined by the three inner classes:  BasicSetter, ArraySetter, and CollectionSetter.
 * This allows us to break up the logic a bit better.</p>
 * 
 * <p>This is always the termination of a setter chain; the {@code next} value is ignored.</p>
 */
public class LeafSetter extends Setter
{
	/** Knows how to set basic noncollection and nonarray values */
	class BasicSetter extends Setter
	{
		@Override
		public void set(Object obj, Object value)
		{
			field.set(obj, importBasic(value, field.getType()));
		}
	}
	
	/** Knows how to set arrays of basic types */
	class ArraySetter extends Setter
	{
		@Override
		public void set(Object obj, Object value)
		{
			if (!(value instanceof Collection<?>))
				throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + field);

			Collection<?> collValue = (Collection<?>)value;

			Class<?> componentType = field.getType().getComponentType();
			
			Object array = Array.newInstance(componentType, collValue.size());

			int index = 0;
			for (Object componentValue: collValue)
			{
				componentValue = importBasic(componentValue, componentType);
				Array.set(array, index++, componentValue);
			}

			field.set(obj, array);
		}
	}

	/**
	 * <p>Deals with collections of basic types.</p>
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
	class CollectionSetter extends Setter
	{
		@Override
		@SuppressWarnings("unchecked")
		public void set(Object obj, Object value)
		{
			if (!(value instanceof Collection<?>))
				throw new IllegalStateException("Cannot load non-collection value '" + value + "' into " + field);

			Collection<?> collValue = (Collection<?>)value;
			Collection<Object> target = (Collection<Object>)field.get(obj);

			if (target != null)
			{
				target.clear();
			}
			else
			{
				if (!field.getType().isInterface())
				{
					target = (Collection<Object>)TypeUtils.class_newInstance(field.getType());
				}
				else if (SortedSet.class.isAssignableFrom(field.getType()))
				{
					target = new TreeSet<Object>();
				}
				else if (Set.class.isAssignableFrom(field.getType()))
				{
					target = new HashSet<Object>();
				}
				else if (List.class.isAssignableFrom(field.getType()) || field.getType().isAssignableFrom(ArrayList.class))
				{
					target = new ArrayList<Object>();
				}
			}

			Class<?> componentType = TypeUtils.getComponentType(field.getType(), field.getGenericType());
			
			for (Object member: collValue)
				target.add(importBasic(member, componentType));

			field.set(obj, target);
		}
	}
	
	/** Need one of these to convert keys */
	ObjectifyFactory factory;
	
	/** The field or method we set */
	Wrapper field;
	
	/** */
	public LeafSetter(ObjectifyFactory fact, Wrapper field)
	{
		this.factory = fact;
		this.field = field;

		if (field.getType().isArray())
		{
			this.next = new ArraySetter();
		}
		else if (Collection.class.isAssignableFrom(field.getType()))
		{
			this.next = new CollectionSetter();
		}
		else
		{
			this.next = new BasicSetter();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void set(Object obj, Object value)
	{
		this.next.set(obj, value);
	}

	/**
	 * Converts a value obtained from the datastore into what gets sent on the field.
	 * The datastore translates values in ways that are not always convenient; for
	 * example, all numbers become Long and booleans become Boolean. This method translates
	 * just the basic types - not collection types.
	 *  
	 * @param fromValue	is the property value that came out of the datastore Entity
	 * @param toType	is the type to convert it to.
	 */
	@SuppressWarnings("unchecked")
	Object importBasic(Object fromValue, Class<?> toType)
	{
		if (fromValue == null)
		{
			return null;
		}
		else if (toType.isAssignableFrom(fromValue.getClass()))
		{
			return fromValue;
		}
		else if (toType == String.class)
		{
			if (fromValue instanceof Text)
				return ((Text) fromValue).getValue();
			else
				return fromValue.toString();
		}
		else if (Enum.class.isAssignableFrom(toType))
		{
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>) toType, fromValue.toString());
		}
		else if ((toType == Boolean.TYPE) && (fromValue instanceof Boolean))
		{
			return fromValue;
		}
		else if (fromValue instanceof Number)
		{
			return coerceNumber((Number) fromValue, toType);
		}
		else if (Key.class.isAssignableFrom(toType) && fromValue instanceof com.google.appengine.api.datastore.Key)
		{
			return this.factory.rawKeyToOKey((com.google.appengine.api.datastore.Key) fromValue);
		}

		throw new IllegalArgumentException("Don't know how to convert " + fromValue.getClass() + " to " + toType);
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	Object coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else if (type == String.class) return value.toString();
		else throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}
}
