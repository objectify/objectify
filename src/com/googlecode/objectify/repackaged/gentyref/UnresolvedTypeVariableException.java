package com.googlecode.objectify.repackaged.gentyref;

import java.lang.reflect.TypeVariable;

@SuppressWarnings("serial")
class UnresolvedTypeVariableException extends RuntimeException {
	private final TypeVariable<?> tv;
	
	UnresolvedTypeVariableException(TypeVariable<?> tv) {
		super("An exact type is requested, but the type contains a type variable that cannot be resolved.\n" +
				"   Variable: " + tv.getName() + " from " + tv.getGenericDeclaration() + "\n" +
				"   Hint: This is usually caused by trying to get an exact type when a generic method who's type parameters are not given is involved.");
		this.tv = tv;
	}
	
	TypeVariable<?> getTypeVariable() {
		return tv;
	}
}
