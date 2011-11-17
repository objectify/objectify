package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Placed on an entity field of type Ref<?> or of an actual entity type, this will cause
 * Objectify to fetch that entity when the containing entity is loaded.</p>
 * 
 * <p>If one or more string values are passed in, these represent fetch groups.  The entity
 * will be fetched only if the fetch group is activated.</p>
 *  
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Load
{
	String[] value() default {};
}