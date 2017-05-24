package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Check whether the property is null or empty
 * 
 * A ValidationException is thrown, if the annotated property is null or empty.
 * 
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface NotNullOrEmpty {
    
}
