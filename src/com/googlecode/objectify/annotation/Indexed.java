package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field (or type), the field(s) will be stored as an indexed property in
 * the datastore. Classes annotated will have fields "indexed by default"; 
 * If no index related annotation is used, this is considered the default.</p>
 * 
 * <p>The specific difference is that, if this annotation is present, fields will be stored
 * with Entity.setProperty() instead of Entity.setUndexedProperty().</p>
 * 
 * @author Scott Hernandez <fullname@gmail)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.TYPE})
public @interface Indexed
{
}