package com.googlecode.objectify.repackaged.gentyref;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * Thrown to indicate that a type argument for a parameterized type is not within the bound declared on the type
 * parameter.
 * 
 * @author Wouter Coekaerts <wouter@coekaerts.be>
 */
@SuppressWarnings("serial")
public class TypeArgumentNotInBoundException extends IllegalArgumentException {
	private final Type argument;
	private final TypeVariable<?> parameter;
	private final Type bound;
	
	public TypeArgumentNotInBoundException(Type argument, TypeVariable<?> parameter, Type bound) {
		super("Given argument [" + argument + "]" +
				" for type parameter [" + parameter.getName() + "] is not within the bound [" + bound + "]");
		this.argument = argument;
		this.parameter = parameter;
		this.bound = bound;
	}

	/**
	 * Returns the supplied argument that is not within the bound.
	 */
	public Type getArgument() {
		return argument;
	}

	/**
	 * Returns the type parameter.
	 */
	public TypeVariable<?> getParameter() {
		return parameter;
	}

	/**
	 * Returns the bound that was not satisfied.
	 * This is one of the members in <tt>getParameter().getBounds()</tt>.
	 */
	public Type getBound() {
		return bound;
	}
}
