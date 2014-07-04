package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Place on one or more fields inside an embedded class. When loaded, the field will be populated
 * with a reference to the containing object. Since embedded classes can nest other classes, this will reference
 * the first object of the correct type when searching up the chain.</p>
 * 
 * <p>Container fields are ignored during save.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Container
{
}