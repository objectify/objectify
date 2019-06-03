package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field of type String, the value will be used as a namespace
 * for saving the entity. The value will be populated when loading the entity.</p>
 *
 * <p>If an entity is saved with a null namespace value, or if an entity does not have
 * a {@code @Namespace}-annotated field, then the namespace from NamespaceManager will
 * be respected.</p>
 *
 * <p>This annotation can only be placed on a single String field within each entity.
 * If the entity also has a {@code @Parent} field, the namespace of the parent key
 * <b>must</b> match the namespace (or the namespace must be null).</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Namespace
{
}