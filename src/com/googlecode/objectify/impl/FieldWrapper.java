package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

/** 
 * Wrapper which makes a field look just like... a field.  More importantly,
 * we can also make methods look just like this. 
 */
public class FieldWrapper implements Wrapper
{
	Field field;
	
	public FieldWrapper(Field field) { this.field = field; }
	
	public Class<?> getType() { return this.field.getType(); }
	
	public Type getGenericType() { return this.field.getGenericType(); }
	
	public void set(Object pojo, Object value)
	{
		try { this.field.set(pojo, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}
	
	public Object get(Object pojo)
	{
		try { return this.field.get(pojo); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}
	
	public String toString()
	{
		return this.field.toString();
	}
}