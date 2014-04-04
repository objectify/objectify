package com.googlecode.objectify.annotation;

import com.googlecode.objectify.condition.If;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation controls whether or not fields are indexed in the datastore.</p>
 * 
 * <p>When placed on a simple field, the field will be indexed if possible.  Text,
 * Blob, and @Serialize fields cannot be indexed.  If the field is an @Embed
 * class or a collection of @Embed classes, the fields of those classes will
 * be indexed as if the class had the @Index annotation.</p>
 * 
 * <p>When placed on an entity class or an embedded class, this sets the default
 * for all fields to be indexed.  It can be overridden by field level annotations.</p>
 * 
 * <p>If an embedded class field is annotated with @Index, any @Index or @Unindex
 * annotation on the class itself is ignored.</p>
 * 
 * <p>If passed one or more classes that implement the {@code If} interface, the
 * value will be indexed only if it tests positive for any of the conditions.  This
 * allows "partial indexing" of only some categories of values (ie, true but not false).</p>
 * 
 * @author Scott Hernandez <fullname@gmail)
 * @author Jeff Schnitzer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Index
{
	// Fully specifying the default value works around a bizarre compiler bug:
	// http://stackoverflow.com/questions/1425088/incompatible-types-found-required-default-enums-in-annotations
	Class<? extends If<?, ?>>[] value() default { com.googlecode.objectify.condition.Always.class };
}