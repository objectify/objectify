package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Ignore;

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
	public static <T> Constructor<T> getNoArgConstructor(Class<T> clazz) {
		try {
			Constructor<T> ctor = clazz.getDeclaredConstructor(new Class[0]);
			ctor.setAccessible(true);
			return ctor;
		}
		catch (NoSuchMethodException e) {
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
	public static <T> Constructor<T> getConstructor(Class<T> clazz, Class<?>... args) {
		try {
			Constructor<T> ctor = clazz.getDeclaredConstructor(args);
			ctor.setAccessible(true);
			return ctor;
		}
		catch (NoSuchMethodException e) {
			throw new IllegalStateException(clazz.getName() + " has no constructor with args " + Arrays.toString(args), e);
		}
	}
	
	/**
	 * Determine if we should create a Property for the field.  Things we ignore:  static, final, @Ignore, synthetic
	 */
	public static boolean isOfInterest(Field field) {
		return !field.isAnnotationPresent(Ignore.class)
			&& ((field.getModifiers() & NOT_SAVEABLE_MODIFIERS) == 0)
			&& !field.isSynthetic();
	}

	/**
	 * Determine if we should create a Property for the method (ie, @AlsoLoad)
	 */
	public static boolean isOfInterest(Method method) {
		for (Annotation[] annos: method.getParameterAnnotations())
			if (getAnnotation(AlsoLoad.class, annos) != null)
				return true;
		
		return false;
	}
	
	/** Checked exceptions are LAME. By the way, don't use this since it causes security exceptions on private classes */
	public static <T> T newInstance(Class<T> clazz) {
		try {
			return clazz.newInstance();
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
	
	/** Checked exceptions are LAME. */
	public static <T> T newInstance(Constructor<T> ctor, Object... params) {
		try {
			return ctor.newInstance(params);
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static Object field_get(Field field, Object obj) {
		try {
			return field.get(obj);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/** Checked exceptions are LAME. */
	public static void field_set(Field field, Object obj, Object value) {
		try {
			field.set(obj, value);
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/**
	 * Get all the persistable fields and methods on a class, checking the superclasses as well.
	 * 
	 * @return the fields we load and save, including @Id and @Parent fields. All fields will be set accessable
	 *  and returned in order starting with superclass fields.
	 */
	public static List<Property> getProperties(Class<?> clazz) {
		List<Property> good = new ArrayList<Property>();
		getProperties(clazz, good);
		return good;
	}

	/** Recursive implementation of getProperties() */
	private static void getProperties(Class<?> clazz, List<Property> good) {
		if (clazz == null || clazz == Object.class)
			return;
		
		getProperties(clazz.getSuperclass(), good);
		
		for (Field field: clazz.getDeclaredFields())
			if (isOfInterest(field))
				good.add(new FieldProperty(field));
		
		for (Method method: clazz.getDeclaredMethods())
			if (isOfInterest(method))
				good.add(new MethodProperty(method));
	}

	/**
	 * A recursive version of Class.getDeclaredField, goes up the hierarchy looking  
	 */
	public static Field getDeclaredField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		try {
			return clazz.getDeclaredField(fieldName);
		}
		catch (NoSuchFieldException ex) {
			if (clazz.getSuperclass() == Object.class)
				throw ex;
			else
				return getDeclaredField(clazz.getSuperclass(), fieldName);
		}
	}

	/**
	 * Just like Class.isAssignableFrom(), but does the right thing when considering autoboxing.
	 */
	public static boolean isAssignableFrom(Class<?> to, Class<?> from) {
		Class<?> notPrimitiveTo = to.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(to) : to;
		Class<?> notPrimitiveFrom = from.isPrimitive() ? PRIMITIVE_TO_WRAPPER.get(from) : from;
		
		return notPrimitiveTo.isAssignableFrom(notPrimitiveFrom);
	}

	/** Gets the annotation that has the specified type, or null if there isn't one */
	@SuppressWarnings("unchecked")
	public static <A extends Annotation> A getAnnotation(Class<A> annotationType, Annotation[] annotations) {
		for (Annotation anno: annotations)
			if (annotationType.isAssignableFrom(anno.getClass()))
				return (A)anno;
		
		return null;
	}

	/** 
	 * Checks both the annotations list and the annotations on the class for the type
	 * @return null if annotation is not in list or on class.
	 */
	public static <A extends Annotation> A getAnnotation(Class<A> annotationType, Annotation[] annotations, Class<?> onClass) {
		A anno = getAnnotation(annotationType, annotations);
		if (anno == null)
			return onClass.getAnnotation(annotationType);
		else
			return anno;
	}
}
