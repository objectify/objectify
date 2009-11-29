package com.googlecode.objectify;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field, the field will be stored as an indexed property in
 * the datastore.  You will not be able to use fields in queries unless they are indexed.</p>
 * 
 * <p>The specific difference is that, if this annotation is present, fields will be stored
 * with Entity.setProperty() instead of Entity.setUnindexedProperty().</p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Indexed
{
}