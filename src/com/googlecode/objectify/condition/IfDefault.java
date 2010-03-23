package com.googlecode.objectify.condition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.googlecode.objectify.impl.TypeUtils;


/**
 * <p>This condition tests against the default value of the field that it
 * is placed upon, <strong>whatever that default may be</strong>.  If you
 * initialize the field with a value, this condition will use that value
 * as the comparison.  For example, if you have a class like this:</p>
 * 
 * <blockquote><pre>
 * public class MyEntity {
 *     &#64;Id Long id;
 *     &#64;NotSaved(IfDefault.class) String foo = "defaultFoo";
 * }
 * </pre></blockquote>
 * 
 * <p>The {@code foo} field will be left unsaved when it has the value "defaultFoo".</p>
 * 
 * <p>Specifically, this conditional constructs an instance of your entity class
 * using the default (no-arg) constructor and stores the default field value for
 * later comparison.  Note that if you initialize the field in your default constructor,
 * this counts!</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfDefault extends ValueIf<Object>
{
	Object defaultValue;
	
	public IfDefault(Class<?> clazz, Field field)
	{
		Constructor<?> ctor = TypeUtils.getNoArgConstructor(clazz);
		Object pojo = TypeUtils.newInstance(ctor);
		this.defaultValue = TypeUtils.field_get(field, pojo);
	}
	
	@Override
	public boolean matches(Object value)
	{
		if (this.defaultValue == null)
			return value == null;
		else
			return this.defaultValue.equals(value);
	}
}