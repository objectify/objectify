package com.googlecode.objectify.util;

import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

/**
 */
public class GenericUtils
{
	private GenericUtils() {
	}

	/**
	 * Get the component type of a Collection.
	 */
	public static Type getCollectionComponentType(Type collectionType) {
		Type componentType = GenericTypeReflector.getTypeParameter(collectionType, Collection.class.getTypeParameters()[0]);
		if (componentType == null)	// if it was a raw type, just assume Object
			return Object.class;
		else
			return componentType;
	}

	/**
	 * Get the key type of a Map.
	 */
	public static Type getMapKeyType(Type mapType) {
		Type componentType = GenericTypeReflector.getTypeParameter(mapType, Map.class.getTypeParameters()[0]);
		if (componentType == null)	// if it was a raw type, just assume Object
			return Object.class;
		else
			return componentType;
	}

	/**
	 * Get the value type of a Map.
	 */
	public static Type getMapValueType(Type mapType) {
		Type componentType = GenericTypeReflector.getTypeParameter(mapType, Map.class.getTypeParameters()[1]);
		if (componentType == null)	// if it was a raw type, just assume Object
			return Object.class;
		else
			return componentType;
	}
}
