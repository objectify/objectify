package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Ignore;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.impl.save.Path;


/**
 */
public class TypeUtils
{
	/** We do not persist fields with any of these modifiers */
	static final int NOT_SAVEABLE_MODIFIERS = Modifier.FINAL | Modifier.STATIC;
	
	/** A map of the primitive types to their wrapper types */
	static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = new HashMap<Class<?>, Class<?>>();
	static {
		PRIMITIVE_TO_WRAPPER.put(boolean.class, Boolean.class);
		PRIMITIVE_TO_WRAPPER.put(byte.class, Byte.class);
		PRIMITIVE_TO_WRAPPER.put(short.class, Short.class);
		PRIMITIVE_TO_WRAPPER.put(int.class, Integer.class);
		PRIMITIVE_TO_WRAPPER.put(long.class, Long.class);
		PRIMITIVE_TO_WRAPPER.put(float.class, Float.class);
		PRIMITIVE_TO_WRAPPER.put(double.class, Double.class);
	}
	
	/**
	 * Throw an IllegalStateException if the class does not have a no-arg constructor.
	 */
	public static <T> Constructor<T> getNoArgConstructor(Class<T> clazz)
	{
		try
		{
			Constructor<T> ctor = clazz.getDeclaredConstructor(new Class[0]);
			ctor.setAccessible(true);
			return ctor;
		}
		catch (NoSuchMethodException e)
		{
			// lame there is no way to tell if the class is a nonstatic inner class
			if (clazz.isMemberClass() || clazz.isAnonymousClass() || clazz.isLocalClass())
				throw new IllegalStateException(clazz.getName() + " must be static and must have a no-arg constructor", e);
			else
				throw new IllegalStateException(clazz.getName() + " must have a no-arg constructor", e);
		}
	}
	
	/**
	 * Gets a constructor that has the specified types of arguments.
	 * Throw an IllegalStateException if the class does not have such a constructor.
	 */
	public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... args)
	{
		try
		{
			Constructor<T> ctor = clazz.getDeclaredConstructor(args);
			ctor.setAccessible(true);
			return ctor;
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalStateException(clazz.getName() + " has no constructor with args " + Arrays.toString(args), e);
		}
	}
	
	/**
	 * @return true if the field can be saved (is persistable), false if it is static, final, @Ignore, etc.
	 */
	public static boolean isSaveable(Field field)
	{
		return !field.isAnnotationPresent(Ignore.class)
			&& ((field.getModifiers() & NOT_SAVEABLE_MODIFIERS) == 0)
			&& !field.isSynthetic();
	}

	/**
	 * Returns the value type, i.e. the argument {@code T} for a generic {@code Map<String, T>}.
	 */
	public static Class<?> getMapValueType(Type genericType)
	{
		if (genericType instanceof ParameterizedType)
		{
			Type[] actualTypeArguments = ((ParameterizedType) genericType).getActualTypeArguments();
			if (actualTypeArguments.length != 2)
			{
				throw new IllegalStateException("Cannot handle subclass of Map with two type arguments");
			}
			Type keyType = actualTypeArguments[0];
			Type valueType = actualTypeArguments[1];
			if (!String.class.equals(keyType))
			{
				throw new IllegalStateException("Map key type must be string");
			}
			if (valueType instanceof Class)
			{
				return (Class<?>) valueType;
			}
			else if (valueType instanceof ParameterizedType)
			{
				return (Class<?>) ((ParameterizedType) valueType).getRawType();
			}
		}
		throw new IllegalStateException("unexpected Map type " + genericType);
	}

	/**
	 * Extend a property path, adding a '.' separator but also checking
	 * for the first element.
	 */
	public static String extendPropertyPath(String prefix, String name)
	{
		if (prefix == null || prefix.length() == 0)
			return name;
		else
			return prefix + '.' + name;
	}
	
	/**
	 * <p>Create a collection of the appropriate type.  The rules are thus:</p>
	 * <ul>
	 * <li>If the type is a concrete collection type, an instance of the concrete type
	 * will be created.</li>
	 * <li>If the type is Set, a HashSet will be created.</li>  
	 * <li>If the type is SortedSet, a TreeSet will be created.</li>  
	 * <li>If the type is List, an ArrayList will be created.</li>  
	 * </ul>
	 * 
	 * @param type is a Collection-derived type
	 */
	@SuppressWarnings("unchecked")
	public static Collection<Object> createCollection(Class<?> type, int size)
	{
		assert Collection.class.isAssignableFrom(type);
		
		if (!type.isInterface())
		{
			return (Collection<Object>)TypeUtils.newInstance(type);
		}
		else if (SortedSet.class.isAssignableFrom(type))
		{
			return new TreeSet<Object>();
		}
		else if (Set.class.isAssignableFrom(type))
		{
			return new HashSet<Object>((int)(size * 1.5));
		}
		else if (List.class.isAssignableFrom(type) || type.isAssignableFrom(ArrayList.class))
		{
			return new ArrayList<Object>(size);
		}
		else
		{
			throw new IllegalStateException("Don't know how to create a collection of type " + type);
		}
	}

	/**
	 * <p>Sets the embedded null indexes property in an entity, which tracks which elements
	 * of a collection are null.  For a base of "foo.bar", the state
	 * property will be "foo.bar^null".  The value, if present, will be a list of indexes
	 * in an embedded collection which are null.</p>
	 * 
	 * <p>If there are no nulls, this property does not need to be set.</p>
	 */
	public static void setNullIndexes(Entity entity, Path subPath, Collection<Integer> value)
	{
		String path = getNullIndexPath(subPath);
		entity.setUnindexedProperty(path, value);
	}
	
	/**
	 * <p>Gets the embedded null indexes property in an entity.</p>
	 * @return null if there is no such property
	 * @see #setNullIndexes(Entity, Path, Collection)
	 */
	@SuppressWarnings("unchecked")
	public static Set<Integer> getNullIndexes(Entity entity, String pathBase)
	{
		String path = getNullIndexPath(pathBase);
		Collection<Number> indexes = (Collection<Number>)entity.getProperty(path);
		if (indexes == null)
		{
			return null;
		}
		else
		{
			// Fucking datastore converts Integers to Longs, but if we're getting this
			// back from the cache then we will get the original Integer.  Evil.
			Set<Integer> result = new HashSet<Integer>();
			for (Number index: indexes)
				result.add(index.intValue());
			
			return result;
		}
	}
	
	/**
	 * @return the path where you will find the null indexes for a base path
	 */
	public static String getNullIndexPath(Path pathBase)
	{
		return pathBase.toPathString() + "^null";
	}
	
	/**
	 * @return the path where you will find the null indexes for a base path
	 */
	public static String getNullIndexPath(String pathBase)
	{
		return pathBase + "^null";
	}
	
	/**
	 * @return true if clazz is an array type or a collection type
	 */
	public static boolean isArrayOrCollection(Class<?> clazz)
	{
		return clazz.isArray() || Collection.class.isAssignableFrom(clazz);
	}
	
	/**
	 * Determines if the field is embedded or not.
	 * 
	 * @return true if field is an embedded class, collection, or array.
	 */
	public static boolean isEmbed(Field field)
	{
		return field.isAnnotationPresent(Embed.class) || field.getType().isAnnotationPresent(Embed.class);
	}
	
	/**
	 * Determines if the field is serialized or not.
	 * 
	 * @return true if field is an embedded class, collection, or array.
	 */
	public static boolean isSerialize(Field field)
	{
		return field.isAnnotationPresent(Embed.class) || field.getType().isAnnotationPresent(Embed.class);
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T newInstance(Class<T> clazz)
	{
		try
		{
			return clazz.newInstance();
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T newInstance(Constructor<T> ctor, Object... params)
	{
		try
		{
			return ctor.newInstance(params);
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }
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

	/**
	 * Get all the loadable fields and methods on a class, checking the superclasses as well.
	 * 
	 * @return the fields we load and save, including @Id and @Parent fields. All fields will be set accessable
	 *  and returned in order starting with superclass fields.
	 */
	public static List<Loadable> getLoadables(Class<?> clazz) {
		List<Loadable> good = new ArrayList<Loadable>();
		getLoadables(clazz, good);
		return good;
	}

	/** Recursive implementation of getLoadables() */
	private static void getLoadables(Class<?> clazz, List<Loadable> good) {
		if (clazz == null || clazz == Object.class)
			return;
		
		getLoadables(clazz.getSuperclass(), good);
		
		for (Field field: clazz.getDeclaredFields()) {
			if (TypeUtils.isSaveable(field)) {
				if (field.isAnnotationPresent(Embed.class) && field.isAnnotationPresent(Serialize.class))
					throw new IllegalStateException("Cannot have @Embed and @Serialize on the same field! Check " + field);

				good.add(new LoadableField(field));
			}
		}
		
		for (Method method: clazz.getDeclaredMethods()) {
			// This seems like a good idea
			if (method.isAnnotationPresent(Embed.class))
				throw new IllegalStateException("@Embed is not a legal annotation for methods");

			for (Annotation[] paramAnnotations: method.getParameterAnnotations())
				for (Annotation ann: paramAnnotations)
					if (ann instanceof AlsoLoad)
						good.add(new LoadableMethod(method, (AlsoLoad)ann));
		}
	}

	/**
	 * A recursive version of Class.getDeclaredField, goes up the hierarchy looking  
	 */
	public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException
	{
		try
		{
			return clazz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException ex)
		{
			if (clazz.getSuperclass() == Object.class)
				throw ex;
			else
				return getDeclaredField(clazz.getSuperclass(), fieldName);
		}
	}

	/**
	 * Get the underlying class for a type, or null if the type is a variable type.
	 * See http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 */
	public static Class<?> getClass(Type type)
	{
		if (type instanceof Class<?>)
		{
			return (Class<?>)type;
		}
		else if (type instanceof ParameterizedType)
		{
			return getClass(((ParameterizedType)type).getRawType());
		}
		else if (type instanceof GenericArrayType)
		{
			Type componentType = ((GenericArrayType)type).getGenericComponentType();
			Class<?> componentClass = getClass(componentType);
			if (componentClass != null)
			{
				return Array.newInstance(componentClass, 0).getClass();
			}
			else
			{
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	/**
	 * Get the actual type arguments a child class has used to extend a generic base class.
	 * See http://www.artima.com/weblogs/viewpost.jsp?thread=208860
	 * This has additionally been modified to handle base interfaces.
	 * 
	 * @param baseClass the base class (or interface)
	 * @param childClass the child class
	 * @return a list of the raw classes for the actual type arguments.
	 */
	public static <T> List<Class<?>> getTypeArguments(Class<T> baseClass, Class<? extends T> childClass)
	{
		Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
		Type type = childClass;
		// start walking up the inheritance hierarchy until we hit baseClass
		while (!getClass(type).equals(baseClass))
		{
			if (type instanceof Class<?>)
			{
				type = climbTypeHierarchy(((Class<?>)type), baseClass);
			}
			else
			{
				ParameterizedType parameterizedType = (ParameterizedType)type;
				Class<?> rawType = (Class<?>)parameterizedType.getRawType();

				Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
				TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
				for (int i = 0; i < actualTypeArguments.length; i++)
				{
					resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
				}

				if (!rawType.equals(baseClass))
				{
					type = climbTypeHierarchy(rawType, baseClass);
				}
			}
		}

		// finally, for each actual type argument provided to baseClass,
		// determine (if possible) the raw class for that type argument.
		Type[] actualTypeArguments;
		if (type instanceof Class<?>)
		{
			actualTypeArguments = ((Class<?>)type).getTypeParameters();
		}
		else
		{
			actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
		}
		
		List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
		// resolve types by chasing down type variables.
		for (Type baseType : actualTypeArguments)
		{
			while (resolvedTypes.containsKey(baseType))
			{
				baseType = resolvedTypes.get(baseType);
			}
			
			typeArgumentsAsClasses.add(getClass(baseType));
		}
		
		return typeArgumentsAsClasses;
	}
	
	/**
	 * Climb the type hierarchy in the direction of parentClass.  Gets the immediate
	 * superclass/superinterface.
	 * 
	 * @return null if parentClass is not in the parent hierarchy of here.
	 */
	public static Type climbTypeHierarchy(Class<?> here, Class<?> parentClass)
	{
		// there is no useful information for us in raw types, so just keep going.
		Type superType = here.getGenericSuperclass();
		
		// It's possible that the baseClass is an interface, not part of the superclass hierarchy.
		// Fortunately we can be guaranteed there is only one of them somewhere in the hierarchy,
		// so we just need to check the type and all the superinterfaces.
		Class<?> superClass = getClass(superType);
		if (parentClass.isAssignableFrom(superClass))
		{
			return superType;
		}
		else
		{
			// Need to find another option in the interfaces
			Type[] interfaceTypes = here.getGenericInterfaces();
			for (int i=0; i<interfaceTypes.length; i++)
			{
				if (parentClass.isAssignableFrom(getClass(interfaceTypes[i])))
				{
					return interfaceTypes[i];
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Just like Class.isAssignableFrom(), but does the right thing when considering autoboxing.
	 */
	public static boolean isAssignableFrom(Class<?> to, Class<?> from)
	{
		Class<?> notPrimitiveTo = to.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(to) : to;
		Class<?> notPrimitiveFrom = from.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(from) : from;
		
		return notPrimitiveTo.isAssignableFrom(notPrimitiveFrom);
	}

	/**
	 * Inspects a specific class and determines if there is a default indexed
	 * state; @Indexed = true, @Unindexed = false, nothing specified = null.
	 */
	public static Boolean isClassIndexed(Class<?> clazz)
	{
		Index indexedAnn = clazz.getAnnotation(Index.class);
		Unindex unindexedAnn = clazz.getAnnotation(Unindex.class);
		
		if (indexedAnn != null && unindexedAnn != null)
		{
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same class: " + clazz.getName());
		}
		
		if (indexedAnn != null && (indexedAnn.value().length != 1 || indexedAnn.value()[0] != Always.class)
				|| unindexedAnn != null && (unindexedAnn.value().length != 1 || unindexedAnn.value()[0] != Always.class))
		{
			throw new IllegalStateException("Class-level @Indexed and @Unindexed annotations cannot have If conditions: " + clazz.getName());
		}
		
		if (indexedAnn != null)
			return true;
		else if (unindexedAnn != null)
			return false;
		else
			return null;
	}

	/** Gets the annotation that has the specified type, or null if there isn't one */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Class<A> type, Annotation[] annotations) {
		for (Annotation anno: annotations)
			if (anno.getClass() == type)
				return (A)anno;
		
		return null;
	}
}
