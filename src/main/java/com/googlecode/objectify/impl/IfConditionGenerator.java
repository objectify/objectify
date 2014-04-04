package com.googlecode.objectify.impl;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;
import com.googlecode.objectify.condition.InitializeIf;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/** 
 * Utility that makes it easy to generate If conditions. 
 */
public class IfConditionGenerator
{
	/** For simple cases that always test positive (ie, empty if arrays) */
	private static final If<?, ?>[] ALWAYS = new If<?, ?>[] { new Always() };

	/** */
	ObjectifyFactory fact;

	/**
	 */
	public IfConditionGenerator(ObjectifyFactory fact) {
		this.fact = fact;
	}
	
	/**
	 * Clever enough to recognize that an empty set of conditions means Always.
	 */
	public If<?, ?>[] generateIfConditions(Class<? extends If<?, ?>>[] ifClasses, Field field) {
		if (ifClasses.length == 0)
			return ALWAYS;

		If<?, ?>[] result = new If<?, ?>[ifClasses.length];
		
		for (int i=0; i<ifClasses.length; i++) {
			
			Class<? extends If<?, ?>> ifClass = ifClasses[i];
			result[i] = this.createIf(ifClass, field);

			// Sanity check the generic If class types to ensure that they match the actual types of the field & entity.
			
			Type valueType = GenericTypeReflector.getTypeParameter(ifClass, If.class.getTypeParameters()[0]);
			Class<?> valueClass = GenericTypeReflector.erase(valueType);
			
			Type pojoType = GenericTypeReflector.getTypeParameter(ifClass, If.class.getTypeParameters()[1]);
			Class<?> pojoClass = GenericTypeReflector.erase(pojoType);
			
			if (!TypeUtils.isAssignableFrom(valueClass, field.getType()))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because you cannot assign " + field.getType().getName() + " to " + valueClass.getName());
			
			if (!TypeUtils.isAssignableFrom(pojoClass, field.getDeclaringClass()))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because the containing class " + field.getDeclaringClass().getName() + " is not compatible with " + pojoClass.getName());
		}
		
		return result;
	}
	
	/** */
	public If<?, ?> createIf(Class<? extends If<?, ?>> ifClass, Field field) {
		If<?, ?> created = fact.construct(ifClass);
		
		if (created instanceof InitializeIf)
			((InitializeIf)created).init(fact, field);
		
		return created;
	}
}