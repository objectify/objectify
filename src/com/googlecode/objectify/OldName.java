package com.googlecode.objectify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation which helps when migrating schemas.  When placed on a field, the property with
 * the oldname will be used to populate the field.  When placed on a method that takes
 * a single parameter, the property with the oldname will be passed to the method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface OldName
{
	String value();
}