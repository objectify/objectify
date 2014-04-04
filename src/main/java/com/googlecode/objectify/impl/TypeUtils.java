package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 */
public class TypeUtils
{
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
		PRIMITIVE_TO_WRAPPER.put(char.class, Character.class);
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
	public static <A extends Annotation> A getAnnotation(Class<A> annotationType, Property prop, Class<?> onClass) {
		A anno = prop.getAnnotation(annotationType);
		if (anno == null)
			return onClass.getAnnotation(annotationType);
		else
			return anno;
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
