package com.googlecode.objectify.repackaged.gentyref;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Utility class for creating instances of {@link Type}.
 * These types can be used with the {@link GenericTypeReflector} or anything else handling Java types.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
public class TypeFactory {
	private static final WildcardType UNBOUND_WILDCARD = new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{});

	private TypeFactory() {
	}

	/**
	 * Creates a type of class <tt>clazz</tt> with <tt>arguments</tt> as type arguments.
	 * <p>
	 * For example: <tt>parameterizedClass(Map.class, Integer.class, String.class)</tt>
	 * returns the type <tt>Map&lt;Integer, String&gt;</tt>.
	 *  
	 * @param clazz Type class of the type to create
	 * @param arguments Type arguments for the variables of <tt>clazz</tt>, or null if these are not known.
	 * @return A {@link ParameterizedType},
	 *         or simply <tt>clazz</tt> if <tt>arguments</tt> is <tt>null</tt> or empty.
	 */
	public static Type parameterizedClass(Class<?> clazz, Type... arguments) {
		return parameterizedInnerClass(null, clazz, arguments);
	}
	
	/**
	 * Creates a type of <tt>clazz</tt> nested in <tt>owner</tt>.
	 * 
	 * @param owner The owner type. This should be a subtype of <tt>clazz.getDeclaringClass()</tt>,
	 * 		or <tt>null</tt> if no owner is known.
	 * @param clazz Type class of the type to create
	 * @return A {@link ParameterizedType} if the class declaring <tt>clazz</tt> is generic and its type parameters
	 * 	are known in <tt>owner</tt> and <tt>clazz</tt> itself has no type parameters.
	 * 	Otherwise, just returns <tt>clazz</tt>.
	 */
	public static Type innerClass(Type owner, Class<?> clazz) {
		return parameterizedInnerClass(owner, clazz, (Type[])null);
	}
	
	/**
	 * Creates a type of <tt>clazz</tt> with <tt>arguments</tt> as type arguments, nested in <tt>owner</tt>.
	 * <p>
	 * In the ideal case, this returns a {@link ParameterizedType} with all generic information in it.
	 * If some type arguments are missing or if the resulting type simply doesn't need any type parameters,
	 * it returns the raw <tt>clazz</tt>.
	 * Note that types with some parameters specified and others not, don't exist in Java.
	 * <p> 
	 * If the caller does not know the exact <tt>owner</tt> type or <tt>arguments</tt>, <tt>null</tt> should be given
	 * (or {@link #parameterizedClass(Class, Type...)} or {@link #innerClass(Type, Class)} could be used).
	 * If they are not needed (non-generic owner and/or <tt>clazz</tt> has no type parameters), they will be filled in
	 * automatically. If they are needed but are not given, the raw <tt>clazz</tt> is returned.
	 * <p>
	 * The specified <tt>owner</tt> may be any subtype of <tt>clazz.getDeclaringClass()</tt>. It is automatically
	 * converted into the right parameterized version of the declaring class.
	 * If <tt>clazz</tt> is a <tt>static</tt> (nested) class, the owner is not used.
	 * 
	 * @param owner The owner type. This should be a subtype of <tt>clazz.getDeclaringClass()</tt>,
	 * 	or <tt>null</tt> if no owner is known.
	 * @param clazz Type class of the type to create
	 * @param arguments Type arguments for the variables of <tt>clazz</tt>, or null if these are not known.
	 * @return A {@link ParameterizedType} if <tt>clazz</tt> or the class declaring <tt>clazz</tt> is generic,
	 * 	and all the needed type arguments are specified in <tt>owner</tt> and <tt>arguments</tt>.
	 * 	Otherwise, just returns <tt>clazz</tt>.
	 * @throws IllegalArgumentException if <tt>arguments</tt> (is non-null and) has an incorrect length,
	 *  or if one of the <tt>arguments</tt> is not within the bounds declared on the matching type variable,
	 * 	or if owner is non-null but <tt>clazz</tt> has no declaring class (e.g. is a top-level class),
	 *  or if owner is not a a subtype of <tt>clazz.getDeclaringClass()</tt>.
	 * @throws NullPointerException if <tt>clazz</tt> or one of the elements in <tt>arguments</tt> is null.
	 */
	public static Type parameterizedInnerClass(Type owner, Class<?> clazz, Type... arguments) {
		// never allow an owner on a class that doesn't have one
		if (clazz.getDeclaringClass() == null && owner != null) {
			throw new IllegalArgumentException("Cannot specify an owner type for a top level class");
		}
		
		Type realOwner = transformOwner(owner, clazz);
		
		if (arguments == null) {
			if (clazz.getTypeParameters().length == 0) {
				// no arguments known, but no needed so just use an empty argument list.
				// (we can still end up with a generic type if the owner is generic)
				arguments = new Type[0];
			} else {
				// missing type arguments, return the raw type
				return clazz;
			}
		} else {
			if (arguments.length != clazz.getTypeParameters().length) {
				throw new IllegalArgumentException("Incorrect number of type arguments for [" + clazz + "]: " +
						"expected " + clazz.getTypeParameters().length + ", but got " + arguments.length);
			}
		}
		
		// if the class and its owner simply have no parameters at all, this is not a parameterized type
		if (!GenericTypeReflector.isMissingTypeParameters(clazz)) {
			return clazz;
		}
		
		// if the owner type is missing type parameters and clazz is non-static, this is a raw type
		if (realOwner != null && !Modifier.isStatic(clazz.getModifiers())
				&& GenericTypeReflector.isMissingTypeParameters(realOwner)) {
			return clazz;
		}
		
		ParameterizedType result = new ParameterizedTypeImpl(clazz, arguments, realOwner);
		checkParametersWithinBound(result);
		return result;
	}
	
	/**
	 * Check if the type arguments of the given type are within the bounds declared on the type parameters.
	 * Only the type arguments of the type itself are checked, the possible owner type is assumed to be valid.
	 * <p>
	 * It does not follow the checks defined in the
	 * <a href="http://java.sun.com/docs/books/jls/third_edition/html/typesValues.html#4.5">JLS</a> because there are
	 * several problems with those (see http://stackoverflow.com/questions/7003009 for one).
	 * Instead, this applies some intuition and follows what Java compilers seem to do. 
	 * 
	 * @param type possibly inconsistent type to check.
	 * @throws IllegalArgumentException if the type arguments are not within the bounds
	 */
	private static void checkParametersWithinBound(ParameterizedType type) {
		Type[] arguments = type.getActualTypeArguments();
		TypeVariable<?>[] typeParameters = ((Class<?>)type.getRawType()).getTypeParameters();
		
		// a map of type arguments in the type, to fill in variables in the bounds 
		VarMap varMap = new VarMap(type);
		
		// for every bound on every parameter
		for (int i = 0; i < arguments.length; i++) {
			for (Type bound : typeParameters[i].getBounds()) {
				// replace type variables in the bound by their value
				Type replacedBound = varMap.map(bound);
				
				
				if (arguments[i] instanceof WildcardType) {
					WildcardType wildcardTypeParameter = (WildcardType) arguments[i];
					
					// Check if a type satisfying both the bounds of the variable and of the wildcard could exist
					
					// upper bounds must not be mutually exclusive
					for (Type wildcardUpperBound : wildcardTypeParameter.getUpperBounds()) {
						if (!couldHaveCommonSubtype(replacedBound, wildcardUpperBound)) {
							throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
						}
					}
					// a lowerbound in the wildcard must satisfy every upperbound 
					for (Type wildcardLowerBound : wildcardTypeParameter.getLowerBounds()) {
						if (!GenericTypeReflector.isSuperType(replacedBound, wildcardLowerBound)) {
							throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
						}
					}
				} else {
					if (! GenericTypeReflector.isSuperType(replacedBound, arguments[i])) {
						throw new TypeArgumentNotInBoundException(arguments[i], typeParameters[i], bound);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if the intersection of two types is not empty.
	 */
	private static boolean couldHaveCommonSubtype(Type type1, Type type2) {
		// this is an optimistically naive implementation.
		// if they are parameterized types their parameters need to be checked,...
		// so we're just a bit too lenient here
		
		Class<?> erased1 = GenericTypeReflector.erase(type1);
		Class<?> erased2 = GenericTypeReflector.erase(type2);
		// if they are both classes
		if (!erased1.isInterface() &&  !erased2.isInterface()) {
			// then one needs to be a subclass of another
			if (!erased1.isAssignableFrom(erased2) && !erased2.isAssignableFrom(erased1)) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Transforms the given owner type into an appropriate one when constructing a parameterized type.
	 */
	private static Type transformOwner(Type givenOwner, Class<?> clazz) {
		if (givenOwner == null) {
			// be lenient: if this is an inner class but no owner was specified, assume a raw owner type
			// (or if there is no owner just return null)
			return clazz.getDeclaringClass();
		} else {
			// If the specified owner is not of the declaring class' type, but instead a subtype,
			// transform it into the declaring class with the exact type parameters.
			// For example with "class StringOuter extends GenericOuter<String>", transform
			// "StringOuter.Inner" into "GenericOuter<String>.Inner", just like the Java compiler does.
			Type transformedOwner = GenericTypeReflector.getExactSuperType(givenOwner, clazz.getDeclaringClass());
			
			if (transformedOwner == null) { // null means it's not a supertype
				throw new IllegalArgumentException("Given owner type [" + givenOwner + "] is not appropriate for ["
						+ clazz + "]: it should be a subtype of " + clazz.getDeclaringClass());
			}
			
			if (Modifier.isStatic(clazz.getModifiers())) {
				// for a static inner class, the owner shouldn't have type parameters
				return GenericTypeReflector.erase(transformedOwner);
			} else {
				return transformedOwner;
			}
		}
	}
	
	/**
	 * Returns the wildcard type without bounds.
	 * This is the '<tt>?</tt>' in for example <tt>List&lt;?&gt</tt>.
	 * 
	 * @return The unbound wildcard type
	 */
	public static WildcardType unboundWildcard() {
		return UNBOUND_WILDCARD;
	}
	
	/**
	 * Creates a wildcard type with an upper bound.
	 * <p>
	 * For example <tt>wildcardExtends(String.class)</tt> returns the type <tt>? extends String</tt>. 
	 * 
	 * @param upperBound Upper bound of the wildcard
	 * @return A wildcard type
	 */
	public static WildcardType wildcardExtends(Type upperBound) {
		if (upperBound == null) {
			throw new NullPointerException();
		}
		return new WildcardTypeImpl(new Type[]{upperBound}, new Type[]{});
	}
	
	/**
	 * Creates a wildcard type with a lower bound.
	 * <p>
	 * For example <tt>wildcardSuper(String.class)</tt> returns the type <tt>? super String</tt>. 
	 * 
	 * @param lowerBound Lower bound of the wildcard
	 * @return A wildcard type
	 */
	public static WildcardType wildcardSuper(Type lowerBound) {
		if (lowerBound == null) {
			throw new NullPointerException();
		}
		return new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{lowerBound});
	}
	
	/**
	 * Creates a array type.
	 * <p>
	 * If <tt>componentType</tt> is not a generic type but a {@link Class} object,
	 * this returns the {@link Class} representing the non-generic array type.
	 * Otherwise, returns a {@link GenericArrayType}.
	 * <p>
	 * For example: <ul>
	 *    <li><tt>arrayOf(String.class)</tt> returns <tt>String[].class</tt></li>
	 *    <li><tt>arrayOf(parameterizedClass(List.class, String.class))</tt> returns the {@link GenericArrayType}
	 *     for <tt>List&lt;String&gt;[]</tt>
	 * </ul>
	 * 
	 * @param componentType The type of the components of the array.
	 * @return An array type.
	 */
	public static Type arrayOf(Type componentType) {
		return GenericArrayTypeImpl.createArrayType(componentType);
	}

}
