package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Check if the property value is one of the given values
 * 
 * A ValidationException is thrown if the value of the annotated property is not
 * in the list of possible values
 * 
 * @author Hendrik Pilz <hepisec@gmail.com>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface PossibleValues {
    String[] value();
}
