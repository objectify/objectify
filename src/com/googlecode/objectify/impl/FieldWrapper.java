package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.googlecode.objectify.annotation.Serialized;

/** 
 * Wrapper which makes a field look just like... a field.  More importantly,
 * we can also make methods look just like this. 
 */
public class FieldWrapper implements Wrapper
{
	Field field;
	
	public FieldWrapper(Field field) { this.field = field; }
	
	@Override
	public Class<?> getType() { return this.field.getType(); }
	
	@Override
	public Type getGenericType() { return this.field.getGenericType(); }
	
	@Override
	public void set(Object pojo, Object value)
	{
		try { this.field.set(pojo, value); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}
	
	@Override
	public Object get(Object pojo)
	{
		try { return this.field.get(pojo); }
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}

	@Override
	public boolean isSerialized()
	{
		return this.field.isAnnotationPresent(Serialized.class);
	}
	
	@Override
	public String toString()
	{
		return this.field.toString();
	}
}