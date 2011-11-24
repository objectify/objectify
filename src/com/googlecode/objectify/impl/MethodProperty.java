package com.googlecode.objectify.impl;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.objectify.annotation.AlsoLoad;

/** 
 * Property which encapsulates a method with an @AlsoLoad parameter. 
 * If you try to get() the value it is always null.
 */
public class MethodProperty implements Property
{
	Method method;
	String[] names;
	Annotation[] annotations;
	
	public MethodProperty(Method method) {
		method.setAccessible(true);
		
		this.method = method;

		// Method must have only one parameter
		if (method.getParameterTypes().length != 1)
			throw new IllegalStateException("@AlsoLoad methods must have a single parameter. Can't use " + method);
		
		annotations = method.getParameterAnnotations()[0];
		
		AlsoLoad alsoLoad = TypeUtils.getAnnotation(AlsoLoad.class, annotations);
		
		if (alsoLoad.value() == null || alsoLoad.value().length == 0)
			throw new IllegalStateException("@AlsoLoad must have a value on " + method);
		
		Set<String> nameSet = new HashSet<String>();
		
		for (String name: alsoLoad.value()) {
			if (name == null || name.trim().length() == 0)
				throw new IllegalStateException("Illegal empty value in @AlsoLoad for " + method);
			
			nameSet.add(name);
		}
		
		names = nameSet.toArray(new String[nameSet.size()]);
	}
	
	@Override
	public String getName() { return method.getName() + "()"; }
	
	@Override
	public String[] getAllNames() { return names; }
	
	@Override
	public Type getType() { return this.method.getGenericParameterTypes()[0]; }

	@Override
	public Annotation[] getAnnotations() {
		return annotations;
	}
	
	@Override
	public void set(Object pojo, Object value) {
		try { this.method.invoke(pojo, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
		catch (InvocationTargetException ex) { throw new RuntimeException(ex); }
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