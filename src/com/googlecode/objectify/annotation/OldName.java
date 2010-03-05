package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Annotation which helps when migrating schemas.  When placed on a field, the property with
 * the oldname will be used to populate the field.  When placed on a parameter to a method
 * that takes a single parameter, the property with the oldname will be passed to the method.</p>
 * 
 * <p>This annotation has been deprecated by {@code @AlsoLoad}</p> 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Deprecated
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.PARAMETER})
public @interface OldName
{
	String value();
}