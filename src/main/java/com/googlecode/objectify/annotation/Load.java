package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Placed on an entity field of type Ref<?>, this will cause
 * Objectify to fetch that entity when the containing entity is loaded.</p>
 *
 * <p>If one or more Class values are passed in, these represent load groups.  The entity
 * will be fetched only if the load group is activated.  The class can be any arbitrary
 * class, and class inheritance is respected.</p>
 *
 * <p>For example, for a class Foo extends Bar, specifying @Load(Bar.class) will cause
 * a field to load if the Foo.class group is enabled.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface Load
{
	/** Groups which indicate the value should be loaded.  Empty means "always". */
	Class<?>[] value() default {};

	/** Groups which negate loading.  In case of conflict with value(), unless() wins. */
	Class<?>[] unless() default {};
}