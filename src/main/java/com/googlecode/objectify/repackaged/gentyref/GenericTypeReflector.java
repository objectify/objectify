package com.googlecode.objectify.repackaged.gentyref;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class for doing reflection on types.
 *
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class GenericTypeReflector {
	private static final Type UNBOUND_WILDCARD = new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{});

	/**
	 * Returns the erasure of the given type.
	 */
	public static Class<?> erase(Type type) {
		if (type instanceof Class) {
			return (Class<?>)type;
		} else if (type instanceof ParameterizedType) {
			return (Class<?>)((ParameterizedType) type).getRawType();
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>)type;
			if (tv.getBounds().length == 0)
				return Object.class;
			else
				return erase(tv.getBounds()[0]);
		} else if (type instanceof GenericArrayType) {
			GenericArrayType aType = (GenericArrayType) type;
			return GenericArrayTypeImpl.createArrayType(erase(aType.getGenericComponentType()));
		} else {
			// TODO at least support CaptureType here
			throw new RuntimeException("not supported: " + type.getClass());
		}
	}

	/**
	 * Maps type parameters in a type to their values.
	 * @param toMapType Type possibly containing type arguments
	 * @param typeAndParams must be either ParameterizedType, or (in case there are no type arguments, or it's a raw type) Class
	 * @return toMapType, but with type parameters from typeAndParams replaced.
	 */
	private static Type mapTypeParameters(Type toMapType, Type typeAndParams) {
		if (isMissingTypeParameters(typeAndParams)) {
			return erase(toMapType);
		} else {
			VarMap varMap = new VarMap();
			Type handlingTypeAndParams = typeAndParams;
			while(handlingTypeAndParams instanceof ParameterizedType) {
				ParameterizedType pType = (ParameterizedType)handlingTypeAndParams;
				Class<?> clazz = (Class<?>)pType.getRawType(); // getRawType should always be Class
				varMap.addAll(clazz.getTypeParameters(), pType.getActualTypeArguments());
				handlingTypeAndParams = pType.getOwnerType();
			}
			return varMap.map(toMapType);
		}
	}

	/**
	 * Checks if the given type is a class that is supposed to have type parameters, but doesn't.
	 * In other words, if it's a really raw type.
	 */
	static boolean isMissingTypeParameters(Type type) {
		if (type instanceof Class) {
			for (Class<?> clazz = (Class<?>) type; clazz != null; clazz = clazz.getEnclosingClass()) {
				if (clazz.getTypeParameters().length != 0)
					return true;
			}
			return false;
		} else if (type instanceof ParameterizedType) {
			return false;
		} else {
			throw new AssertionError("Unexpected type " + type.getClass());
		}
	}

	/**
	 * Returns a type representing the class, with all type parameters the unbound wildcard ("?").
	 * For example, <tt>addWildcardParameters(Map.class)</tt> returns a type representing <tt>Map&lt;?,?&gt;</tt>.
	 * @return <ul>
	 * <li>If clazz is a class or interface without type parameters, clazz itself is returned.</li>
	 * <li>If clazz is a class or interface with type parameters, an instance of ParameterizedType is returned.</li>
	 * <li>if clazz is an array type, an array type is returned with unbound wildcard parameters added in the the component type.
	 * </ul>
	 */
	public static Type addWildcardParameters(Class<?> clazz) {
		if (clazz.isArray()) {
			return GenericArrayTypeImpl.createArrayType(addWildcardParameters(clazz.getComponentType()));
		} else if (isMissingTypeParameters(clazz)) {
			TypeVariable<?>[] vars = clazz.getTypeParameters();
			Type[] arguments = new Type[vars.length];
			Arrays.fill(arguments, UNBOUND_WILDCARD);
			Type owner = clazz.getDeclaringClass() == null ? null : addWildcardParameters(clazz.getDeclaringClass());
			return new ParameterizedTypeImpl(clazz, arguments, owner);
		} else {
			return clazz;
		}
	}

	/**
	 * Finds the most specific supertype of <tt>type</tt> whose erasure is <tt>searchClass</tt>.
	 * In other words, returns a type representing the class <tt>searchClass</tt> plus its exact type parameters in <tt>type</tt>.
	 *
	 * <ul>
	 * <li>Returns an instance of {@link ParameterizedType} if <tt>searchClass</tt> is a real class or interface and <tt>type</tt> has parameters for it</li>
	 * <li>Returns an instance of {@link GenericArrayType} if <tt>searchClass</tt> is an array type, and <tt>type</tt> has type parameters for it</li>
	 * <li>Returns an instance of {@link Class} if <tt>type</tt> is a raw type, or has no type parameters for <tt>searchClass</tt></li>
	 * <li>Returns null if <tt>searchClass</tt> is not a superclass of type.</li>
	 * </ul>
	 *
	 * <p>For example, with <tt>class StringList implements List&lt;String&gt;</tt>, <tt>getExactSuperType(StringList.class, Collection.class)</tt>
	 * returns a {@link ParameterizedType} representing <tt>Collection&lt;String&gt;</tt>.
	 * </p>
	 */
	public static Type getExactSuperType(Type type, Class<?> searchClass) {
		if (type instanceof ParameterizedType || type instanceof Class || type instanceof GenericArrayType) {
			Class<?> clazz = erase(type);

			if (searchClass == clazz) {
				return type;
			}

			if (! searchClass.isAssignableFrom(clazz))
				return null;
		}

		for (Type superType: getExactDirectSuperTypes(type)) {
			Type result = getExactSuperType(superType, searchClass);
			if (result != null)
				return result;
		}

		return null;
	}

	/**
	 * Gets the type parameter for a given type that is the value for a given type variable.
	 * For example, with <tt>class StringList implements List&lt;String&gt;</tt>,
	 * <tt>getTypeParameter(StringList.class, Collection.class.getTypeParameters()[0])</tt>
	 * returns <tt>String</tt>.
	 *
	 * @param type The type to inspect.
	 * @param variable The type variable to find the value for.
	 * @return The type parameter for the given variable. Or null if type is not a subtype of the
	 * 	type that declares the variable, or if the variable isn't known (because of raw types).
	 */
	public static Type getTypeParameter(Type type, TypeVariable<? extends Class<?>> variable) {
		Class<?> clazz = variable.getGenericDeclaration();
		Type superType = getExactSuperType(type, clazz);
		if (superType instanceof ParameterizedType) {
			int index = Arrays.asList(clazz.getTypeParameters()).indexOf(variable);
			return ((ParameterizedType)superType).getActualTypeArguments()[index];
		} else {
			return null;
		}
	}

	/**
	 * Checks if the capture of subType is a subtype of superType
	 */
	public static boolean isSuperType(Type superType, Type subType) {
		if (superType instanceof ParameterizedType || superType instanceof Class || superType instanceof GenericArrayType) {
			Class<?> superClass = erase(superType);
			Type mappedSubType = getExactSuperType(capture(subType), superClass);
			if (mappedSubType == null) {
				return false;
			} else if (superType instanceof Class<?>) {
				return true;
			} else if (mappedSubType instanceof Class<?>) {
				// TODO treat supertype by being raw type differently ("supertype, but with warnings")
				return true; // class has no parameters, or it's a raw type
			} else if (mappedSubType instanceof GenericArrayType) {
				Type superComponentType = getArrayComponentType(superType);
				assert superComponentType != null;
				Type mappedSubComponentType = getArrayComponentType(mappedSubType);
				assert mappedSubComponentType != null;
				return isSuperType(superComponentType, mappedSubComponentType);
			} else {
				assert mappedSubType instanceof ParameterizedType;
				ParameterizedType pMappedSubType = (ParameterizedType) mappedSubType;
				assert pMappedSubType.getRawType() == superClass;
				ParameterizedType pSuperType = (ParameterizedType)superType;

				Type[] superTypeArgs = pSuperType.getActualTypeArguments();
				Type[] subTypeArgs = pMappedSubType.getActualTypeArguments();
				assert superTypeArgs.length == subTypeArgs.length;
				for (int i = 0; i < superTypeArgs.length; i++) {
					if (! contains(superTypeArgs[i], subTypeArgs[i])) {
						return false;
					}
				}
				// params of the class itself match, so if the owner types are supertypes too, it's a supertype.
				return pSuperType.getOwnerType() == null || isSuperType(pSuperType.getOwnerType(), pMappedSubType.getOwnerType());
			}
		} else if (superType instanceof CaptureType) {
			if (superType.equals(subType))
				return true;
			for (Type lowerBound : ((CaptureType) superType).getLowerBounds()) {
				if (isSuperType(lowerBound, subType)) {
					return true;
				}
			}
			return false;
		} else if (superType instanceof GenericArrayType) {
			return isArraySupertype(superType, subType);
		} else {
			throw new RuntimeException("not implemented: " + superType.getClass());
		}
	}

	private static boolean isArraySupertype(Type arraySuperType, Type subType) {
		Type superTypeComponent = getArrayComponentType(arraySuperType);
		assert superTypeComponent != null;
		Type subTypeComponent = getArrayComponentType(subType);
		if (subTypeComponent == null) { // subType is not an array type
			return false;
		} else {
			return isSuperType(superTypeComponent, subTypeComponent);
		}
	}

	/**
	 * If type is an array type, returns the type of the component of the array.
	 * Otherwise, returns null.
	 */
	public static Type getArrayComponentType(Type type) {
		if (type instanceof Class) {
			Class<?> clazz = (Class<?>)type;
			return clazz.getComponentType();
		} else if (type instanceof GenericArrayType) {
			GenericArrayType aType = (GenericArrayType) type;
			return aType.getGenericComponentType();
		} else {
			return null;
		}
	}

	private static boolean contains(Type containingType, Type containedType) {
		if (containingType instanceof WildcardType) {
			WildcardType wContainingType = (WildcardType)containingType;
			for (Type upperBound : wContainingType.getUpperBounds()) {
				if (! isSuperType(upperBound, containedType)) {
					return false;
				}
			}
			for (Type lowerBound : wContainingType.getLowerBounds()) {
				if (! isSuperType(containedType, lowerBound)) {
					return false;
				}
			}
			return true;
		} else {
			return containingType.equals(containedType);
		}
	}

	/**
	 * Returns the direct supertypes of the given type. Resolves type parameters.
	 */
	private static Type[] getExactDirectSuperTypes(Type type) {
		if (type instanceof ParameterizedType || type instanceof Class) {
			Class<?> clazz;
			if (type instanceof ParameterizedType) {
				clazz = (Class<?>)((ParameterizedType)type).getRawType();
			} else {
				// TODO primitive types?
				clazz = (Class<?>)type;
				if (clazz.isArray())
					return getArrayExactDirectSuperTypes(clazz);
			}

			Type[] superInterfaces = clazz.getGenericInterfaces();
			Type superClass = clazz.getGenericSuperclass();

			// the only supertype of an interface without superinterfaces is Object
			if (superClass == null && superInterfaces.length == 0 && clazz.isInterface()) {
				return new Type[] {Object.class};
			}

			Type[] result;
			int resultIndex;
			if (superClass == null) {
				result = new Type[superInterfaces.length];
				resultIndex = 0;
			} else {
				result = new Type[superInterfaces.length + 1];
				resultIndex = 1;
				result[0] = mapTypeParameters(superClass, type);
			}
			for (Type superInterface : superInterfaces) {
				result[resultIndex++] = mapTypeParameters(superInterface, type);
			}

			return result;
		} else if (type instanceof TypeVariable) {
			TypeVariable<?> tv = (TypeVariable<?>) type;
			return tv.getBounds();
		} else if (type instanceof WildcardType) {
			// This should be a rare case: normally this wildcard is already captured.
			// But it does happen if the upper bound of a type variable contains a wildcard
			// TODO shouldn't upper bound of type variable have been captured too? (making this case impossible?)
			return ((WildcardType) type).getUpperBounds();
		} else if (type instanceof CaptureType) {
			return ((CaptureType)type).getUpperBounds();
		} else if (type instanceof GenericArrayType) {
			return getArrayExactDirectSuperTypes(type);
		} else if (type == null) {
			throw new NullPointerException();
		} else {
			throw new RuntimeException("not implemented type: " + type);
		}
	}

	private static Type[] getArrayExactDirectSuperTypes(Type arrayType) {
		// see http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.10.3
		Type typeComponent = getArrayComponentType(arrayType);

		Type[] result;
		int resultIndex;
		if (typeComponent instanceof Class && ((Class<?>)typeComponent).isPrimitive()) {
			resultIndex = 0;
			result = new Type[3];
		} else {
			Type[] componentSupertypes = getExactDirectSuperTypes(typeComponent);
			result = new Type[componentSupertypes.length + 3];
			for (resultIndex = 0; resultIndex < componentSupertypes.length; resultIndex++) {
				result[resultIndex] = GenericArrayTypeImpl.createArrayType(componentSupertypes[resultIndex]);
			}
		}
		result[resultIndex++] = Object.class;
		result[resultIndex++] = Cloneable.class;
		result[resultIndex++] = Serializable.class;
		return result;
	}

	/**
	 * Returns the exact return type of the given method in the given type.
	 * This may be different from <tt>m.getGenericReturnType()</tt> when the method was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in the return type, or <tt>type</tt> is a raw type.
	 */
	public static Type getExactReturnType(Method m, Type type) {
		Type returnType = m.getGenericReturnType();
		Type exactDeclaringType = getExactSuperType(capture(type), m.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of m.getDeclaringClass()
			throw new IllegalArgumentException("The method " + m + " is not a member of type " + type);
		}
		return mapTypeParameters(returnType, exactDeclaringType);
	}

	/**
	 * Returns the exact type of the given field in the given type.
	 * This may be different from <tt>f.getGenericType()</tt> when the field was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in the type of the field, or <tt>type</tt> is a raw type.
	 */
	public static Type getExactFieldType(Field f, Type type) {
		Type returnType = f.getGenericType();
		Type exactDeclaringType = getExactSuperType(capture(type), f.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of f.getDeclaringClass()
			throw new IllegalArgumentException("The field " + f + " is not a member of type " + type);
		}
		return mapTypeParameters(returnType, exactDeclaringType);
	}

	/**
	 * Returns the exact parameter types of the given method in the given type.
	 * This may be different from <tt>m.getGenericParameterTypes()</tt> when the method was declared in a superclass,
	 * or <tt>type</tt> has a type parameter that is used in one of the parameters, or <tt>type</tt> is a raw type.
	 */
	public static Type[] getExactParameterTypes(Method m, Type type) {
		Type[] parameterTypes = m.getGenericParameterTypes();
		Type exactDeclaringType = getExactSuperType(capture(type), m.getDeclaringClass());
		if (exactDeclaringType == null) { // capture(type) is not a subtype of m.getDeclaringClass()
			throw new IllegalArgumentException("The method " + m + " is not a member of type " + type);
		}

		Type[] result = new Type[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			result[i] = mapTypeParameters(parameterTypes[i], exactDeclaringType);
		}
		return result;
	}

	/**
	 * Applies capture conversion to the given type.
	 */
	public static Type capture(Type type) {
		if (type instanceof ParameterizedType) {
			return capture((ParameterizedType)type);
		} else {
			return type;
		}
	}

	/**
	 * Applies capture conversion to the given type.
	 * @see #capture(Type)
	 */
	public static ParameterizedType capture(ParameterizedType type) {
		// the map from parameters to their captured equivalent

		VarMap varMap = new VarMap();
		// list of CaptureTypes we've created but aren't fully initialized yet
		// we can only initialize them *after* we've fully populated varMap
		List<CaptureTypeImpl> toInit = new ArrayList<>();

		Class<?> clazz = (Class<?>)type.getRawType();
		Type[] arguments = type.getActualTypeArguments();
		TypeVariable<?>[] vars = clazz.getTypeParameters();
		Type[] capturedArguments = new Type[arguments.length];

		assert arguments.length == vars.length; // NICE throw an explaining exception

		for (int i = 0; i < arguments.length; i++) {
			Type argument = arguments[i];
			if (argument instanceof WildcardType) {
				CaptureTypeImpl captured = new CaptureTypeImpl((WildcardType)argument, vars[i]);
				argument = captured;
				toInit.add(captured);
			}
			capturedArguments[i] = argument;
			varMap.add(vars[i], argument);
		}
		for (CaptureTypeImpl captured : toInit) {
			captured.init(varMap);
		}
		Type ownerType = (type.getOwnerType() == null) ? null : capture(type.getOwnerType());
		return new ParameterizedTypeImpl(clazz, capturedArguments, ownerType);
	}

	/**
	 * Returns the display name of a Type.
	 */
	public static String getTypeName(Type type) {
		if(type instanceof Class) {
			Class<?> clazz = (Class<?>) type;
			return clazz.isArray() ? (getTypeName(clazz.getComponentType()) + "[]") : clazz.getName();
		} else {
			return type.toString();
		}
	}

	/**
	 * Returns list of classes and interfaces that are supertypes of the given type.
	 * For example given this class:
	 * <tt>class Foo&lt;A extends Number & Iterable&lt;A&gt;, B extends A&gt;</tt><br>
	 * calling this method on type parameters <tt>B</tt> (<tt>Foo.class.getTypeParameters()[1]</tt>)
	 * returns a list containing <tt>Number</tt> and <tt>Iterable</tt>.
	 * <p>
	 * This is mostly useful if you get a type from one of the other methods in <tt>GenericTypeReflector</tt>,
	 * but you don't want to deal with all the different sorts of types,
	 * and you are only really interested in concrete classes and interfaces.
	 * </p>
	 *
	 * @return A List of classes, each of them a supertype of the given type.
	 * 	If the given type is a class or interface itself, returns a List with just the given type.
	 *  The list contains no duplicates, and is ordered in the order the upper bounds are defined on the type.
	 */
	public static List<Class<?>> getUpperBoundClassAndInterfaces(Type type) {
		LinkedHashSet<Class<?>> result = new LinkedHashSet<>();
		buildUpperBoundClassAndInterfaces(type, result);
		return new ArrayList<>(result);
	}

	/**
	 * Helper method for getUpperBoundClassAndInterfaces, adding the result to the given set.
	 */
	private static void buildUpperBoundClassAndInterfaces(Type type, Set<Class<?>> result) {
		if (type instanceof ParameterizedType || type instanceof Class<?>) {
			result.add(erase(type));
			return;
		}

		for (Type superType: getExactDirectSuperTypes(type)) {
			buildUpperBoundClassAndInterfaces(superType, result);
		}
	}
}
