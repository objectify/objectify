package com.googlecode.objectify.annotation;

import com.googlecode.objectify.condition.If;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field, the field will not be written to the datastore.
 * It will, however, be loaded normally.  This is particularly useful in concert with
 * {@code @OnLoad} and {@code @OnSave} to transform your data.</p>
 * 
 * <p>If passed one or more classes that implement the {@code If} interface, the
 * value will be ignored only if it tests positive for any of the conditions.  This
 * is a convenient way to prevent storing of default values, potentially saving
 * a significant amount of storage and indexing cost.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface IgnoreSave
{
	// Fully specifying the default value works around a bizarre compiler bug:
	// http://stackoverflow.com/questions/1425088/incompatible-types-found-required-default-enums-in-annotations
	Class<? extends If<?, ?>>[] value() default { com.googlecode.objectify.condition.Always.class };
}