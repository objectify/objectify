package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field (or class), the field(s) will be stored as an unindexed property in
 * the datastore. Classes annotated will have fields "unindexed by default" unless overridden at the field level.</p>
 * 
 * <p>The specific difference is that, if this annotation is present, fields will be stored
 * with Entity.setUnindexedProperty() instead of Entity.setProperty().</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 * @author Scott Hernandez <fullname@gmail)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Unindexed
{
}