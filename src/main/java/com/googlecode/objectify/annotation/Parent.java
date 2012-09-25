package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field of type Key, the key will be used as the parent
 * ancestor for entity grouping.</p>
 * 
 * <p>This annotation can only be placed on a single Key field within each entity.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Parent
{
}