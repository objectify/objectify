package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field, the field will not be written to the datastore.
 * It will, however, be loaded normally.  This is particularly useful in concert with
 * {@code @PostLoad} and {@code @PrePersist} to transform your data.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface LoadOnly
{
}