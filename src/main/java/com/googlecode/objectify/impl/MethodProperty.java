package com.googlecode.objectify.impl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/** 
 * Property which encapsulates a method with an @AlsoLoad parameter. 
 * If you try to get() the value it is always null.
 */
public class MethodProperty extends AbstractProperty
{
	/** */
	Method method;
	MethodHandle methodHandle;

	/** */
	public MethodProperty(Method method) {
		super(method.getName() + "()", method.getParameterAnnotations()[0], method);

		method.setAccessible(true);
		this.method = method;

		try {
			this.methodHandle = MethodHandles.lookup().unreflect(method);
		}
		catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}

		// Method must have only one parameter
		if (method.getParameterTypes().length != 1)
			throw new IllegalStateException("@AlsoLoad methods must have a single parameter. Can't use " + method);
	}
	
	@Override
	public Type getType() { return this.method.getGenericParameterTypes()[0]; }

	@Override
	public void set(Object pojo, Object value) {
		try {
			//this.method.invoke(pojo, value);
			methodHandle.invoke(pojo, value);
		}
		catch (RuntimeException ex) { throw ex; }
		catch (Throwable ex) { throw new RuntimeException(ex); }
	}
	
	@Override
	public Object get(Object pojo) {
		return null;	// can't get values from methods
	}
	
	@Override
	public String toString() {
		return this.method.toString();
	}

	/** Never saved */
	@Override
	public boolean isSaved(Object onPojo) {
		return false;
	}

	/** Since we are never saved this is never called */
	@Override
	public Boolean getIndexInstruction(Object onPojo) {
		throw new UnsupportedOperationException("This should never have been called!");
	}
}