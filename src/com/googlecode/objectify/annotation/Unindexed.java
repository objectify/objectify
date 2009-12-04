package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field, the field will be stored as an unindexed property in
 * the datastore.  You will not be able to use these fields in queries.</p>
 * 
 * <p>The specific difference is that, if this annotation is present, fields will be stored
 * with Entity.setUnindexedProperty() instead of Entity.setProperty().</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Unindexed
{
}