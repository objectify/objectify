package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on a type class, fields of that type will be broken down and stored in a dot-separated
 * embedded format in the containing entity.  Unlike serialization, the embedded
 * data is not opaque to the datastore and CAN be indexed.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Embed
{
}