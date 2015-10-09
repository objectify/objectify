package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on a method on a POJO entity, that method will be called just after being loaded from the
 * datastore.  Analogous to the JPA @PostLoad annotation.</p>
 *
 * <p>The method can optionally receive one parameter, an Objectify instance.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface OnLoad
{
}
