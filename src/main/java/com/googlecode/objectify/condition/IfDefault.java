package com.googlecode.objectify.condition;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

import java.lang.reflect.Field;


/**
 * <p>This condition tests against the default value of the field that it
 * is placed upon, <strong>whatever that default may be</strong>.  If you
 * initialize the field with a value, this condition will use that value
 * as the comparison.  For example, if you have a class like this:</p>
 * 
 * <blockquote><pre>
 * public class MyEntity {
 *     &#64;Id Long id;
 *     &#64;IgnoreSave(IfDefault.class) String foo = "defaultFoo";
 * }
 * </pre></blockquote>
 * 
 * <p>The {@code foo} field will be left unsaved when it has the value "defaultFoo".</p>
 * 
 * <p>Specifically, this conditional constructs an instance of <i>the class in which
 * the field is declared</i> and stores the default field value for later comparison.
 * Note that if you initialize the field in your default constructor, this counts!</p>
 *
 * <p>There is one important caveat: Objectify treats each declared class in a type
 * hierarchy separately. The class in which the field is declared have a no-arg constructor,
 * and it alone is used to determine the default value. This will NOT work:</p>
 *
 * <blockquote><pre>
 * public class MyBase {
 *     &#64;Id Long id;
 *     &#64;IgnoreSave(IfDefault.class) String foo = "baseFoo";
 * }
 * public class MyEntity extends MyBase {
 *     public MyEntity() {
 *         foo = "subclassFoo";
 *     }
 * }
 * </pre></blockquote>
 *
 * <p>In this example, the default value will be "baseFoo".</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfDefault extends ValueIf<Object> implements InitializeIf
{
	private Object defaultValue;
	
	@Override
	public void init(final ObjectifyFactory fact, final Field field) {
		final Object pojo = fact.construct(field.getDeclaringClass());
		this.defaultValue = TypeUtils.field_get(field, pojo);
	}
	
	@Override
	public boolean matchesValue(Object value) {
		if (this.defaultValue == null)
			return value == null;
		else
			return this.defaultValue.equals(value);
	}
}