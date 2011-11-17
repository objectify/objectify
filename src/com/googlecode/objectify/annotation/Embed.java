package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field which is a compound type or a collection of compound types), the object(s)
 * will be broken down into parts and stored in the containing entity.  Unlike serialization, the embedded
 * data is not opaque to the datastore and CAN be indexed.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Embed
{
}