package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation must be placed on your entity POJOs.  If you have a polymorphic hierarchy,
 * the root should have @Entity and the subclasses should have @Subclass.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Inherited
public @interface Entity
{
	/**
	 * Controls the actual kind name used in the datastore.
	 */
	String name() default "";
}