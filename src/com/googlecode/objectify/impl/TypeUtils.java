package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Transient;

import com.googlecode.objectify.impl.load.Wrapper;

/**
 */
public class TypeUtils
{
	/** We do not persist fields with any of these modifiers */
	static final int NOT_SAVED_MODIFIERS = Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT;

	/**
	 * Throw an IllegalStateException if the class does not have a no-arg constructor.
	 */
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
	 * @return true if the field can be saved (is persistable), false if it
	 *  is static, final, transient, etc.
	 */
	public static boolean isSaveable(Field field)
	{
		return !field.isAnnotationPresent(Transient.class)
			&& ((field.getModifiers() & NOT_SAVED_MODIFIERS) == 0);
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
	 * <p>Prepare a collection of the appropriate type and place it on the pojo's field.
	 * The rules are thus:</p>
	 * <ul>
	 * <li>If the field already contains a collection object, it will be returned.
	 * A new instance will not be created.</li>
	 * <li>If the field is a concrete collection type, an instance of the concrete type
	 * will be created.</li>
	 * <li>If the field is Set, a HashSet will be created.</li>  
	 * <li>If the field is SortedSet, a TreeSet will be created.</li>  
	 * <li>If the field is List, an ArrayList will be created.</li>  
	 * </ul>
	 * 
	 * @param field is a Collection-derived field on the pojo.
	 * @param onPojo is the object whose field should be set 
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> prepareCollection(Object onPojo, Wrapper collectionField)
	{
		assert Collection.class.isAssignableFrom(collectionField.getType());
		
		Collection<Object> coll = (Collection<Object>)collectionField.get(onPojo);

		if (coll != null)
		{
			return coll;
		}
		else
		{
			if (!collectionField.getType().isInterface())
			{
				coll = (Collection<Object>)TypeUtils.class_newInstance(collectionField.getType());
			}
			else if (SortedSet.class.isAssignableFrom(collectionField.getType()))
			{
				coll = new TreeSet<Object>();
			}
			else if (Set.class.isAssignableFrom(collectionField.getType()))
			{
				coll = new HashSet<Object>();
			}
			else if (List.class.isAssignableFrom(collectionField.getType()) || collectionField.getType().isAssignableFrom(ArrayList.class))
			{
				coll = new ArrayList<Object>();
			}
		}
		
		collectionField.set(onPojo, coll);
		
		return coll;
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T class_newInstance(Class<T> clazz)
	{
		try
		{
			return clazz.newInstance();
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static Object field_get(Field field, Object obj)
	{
		try
		{
			return field.get(obj);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static void field_set(Field field, Object obj, Object value)
	{
		try
		{
			field.set(obj, value);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

}
