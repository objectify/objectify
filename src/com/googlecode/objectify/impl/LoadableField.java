package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.googlecode.objectify.annotation.AlsoLoad;

/** 
 * Loadable which encapsulates a simple field. 
 */
public class LoadableField implements Loadable
{
	String[] names;
	Field field;
	
	public LoadableField(Field field) {
		this.field = field;
		
		field.setAccessible(true);
		
		Set<String> nameSet = new HashSet<String>();
		nameSet.add(field.getName());
		
		// Now any additional names, either @AlsoLoad or the deprecated @OldName
		AlsoLoad alsoLoad = field.getAnnotation(AlsoLoad.class);
		if (alsoLoad != null)
			if (alsoLoad.value() == null || alsoLoad.value().length == 0)
				throw new IllegalStateException("If specified, @AlsoLoad must specify at least one value: " + field);
			else
				for (String value: alsoLoad.value())
					if (value == null || value.trim().length() == 0)
						throw new IllegalStateException("Illegal value '" + value + "' in @AlsoLoad for " + field);
					else
						nameSet.add(value);
		
		names = nameSet.toArray(new String[nameSet.size()]);
	}
	
	@Override
	public String[] getNames() { return names; }
	
	@Override
	public Type getType() { return this.field.getGenericType(); }
	
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
	public boolean isSerialize()
	{
		return TypeUtils.isSerialize(field);
	}
	
	@Override
	public boolean isEmbed()
	{
		return TypeUtils.isEmbed(field);
	}

	@Override
	public String toString()
	{
		return this.field.toString();
	}

}