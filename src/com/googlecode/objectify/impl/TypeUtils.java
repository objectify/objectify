package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 */
public class TypeUtils
{
	public static void checkForNoArgConstructor(Class<?> clazz)
	{
		try
		{
			clazz.getConstructor(new Class[0]);
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException("There must be a public no-arg constructor for " + clazz.getName(), e);
		}
	}

	/**
	 * If getType() is an array or Collection, returns the component type - otherwise null
	 */
	public static Class<?> getComponentType(Class<?> type, Type genericType)
	{
		if (type.isArray())
		{
			return type.getComponentType();
		}
		else if (Collection.class.isAssignableFrom(type))
		{
			while (genericType instanceof Class<?>)
				genericType = ((Class<?>) genericType).getGenericSuperclass();

			if (genericType instanceof ParameterizedType)
			{
				Type actualTypeArgument = ((ParameterizedType) genericType).getActualTypeArguments()[0];
				if (actualTypeArgument instanceof Class<?>)
					return (Class<?>) actualTypeArgument;
				else if (actualTypeArgument instanceof ParameterizedType)
					return (Class<?>) ((ParameterizedType) actualTypeArgument).getRawType();
				else
					return null;
			}
			else
			{
				return null;
			}
		}
		else	// not array or collection
		{
			return null;
		}
	}

	/**
	 * Converts the value into an object suitable for the type (hopefully).
	 * For loading data out of the datastore.  Note that collections are
	 * quite complicated since they are always written as List<?>.
	 *
	 * @param value		 is the property value that came out of the datastore Entity
	 * @param type		  is the type of the field or method param we are populating
	 * @param componentType is the type of a component of 'type' when 'type' is
	 *                      an array or collection.  null if 'type' is not an array or collection.
	 */
	@SuppressWarnings("unchecked")
	public static Object convertFromDatastore(ObjectifyFactory factory, Object value, Class<?> type, Class<?> componentType) throws IllegalAccessException, InstantiationException
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof Collection<?>)
		{
			Collection<?> collValue = (Collection<?>) value;

			if (type.isArray())
			{
				// The objects in the Collection are assumed to be of correct type for the array
				Object array = Array.newInstance(componentType, collValue.size());

				int index = 0;
				for (Object componentValue : collValue)
				{
					componentValue = convertFromDatastore(factory, componentValue, componentType, null);

					//System.out.println("componentType is " + componentType + ", componentValue class is " + componentValue.getClass());
					Array.set(array, index++, componentValue);
				}

				return array;
			}
			else if (Collection.class.isAssignableFrom(type)) // Check for collection early!
			{
				// We're making some sort of collection.  If it's a concrete class, just
				// instantiate it.  Otherwise it's an interface and we need to pick the
				// concrete class ourselves.
				Collection<Object> target = null;

				if (!type.isInterface())
				{
					target = (Collection<Object>) type.newInstance();
				}
				else if (SortedSet.class.isAssignableFrom(type))
				{
					target = new TreeSet<Object>();
				}
				else if (Set.class.isAssignableFrom(type))
				{
					target = new HashSet<Object>();
				}
				else if (List.class.isAssignableFrom(type) || type.isAssignableFrom(ArrayList.class))
				{
					target = new ArrayList<Object>();
				}

				for (Object obj : collValue)
					target.add(convertFromDatastore(factory, obj, componentType, null));

				return target;
			}
		}
		else if (type.isAssignableFrom(value.getClass()))
		{
			return value;
		}
		else if (type == String.class)
		{
			if (value instanceof Text)
				return ((Text) value).getValue();
			else
				return value.toString();
		}
		else if (Enum.class.isAssignableFrom(type))
		{
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>) type, value.toString());
		}
		else if ((value instanceof Boolean) && (type == Boolean.TYPE))
		{
			return value;
		}
		else if (value instanceof Number)
		{
			return coerceNumber((Number) value, type);
		}
		else if (value instanceof com.google.appengine.api.datastore.Key && Key.class.isAssignableFrom(type))
		{
			return factory.rawKeyToOKey((com.google.appengine.api.datastore.Key) value);
		}

		throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	static Object coerceNumber(Number value, Class<?> type)
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
