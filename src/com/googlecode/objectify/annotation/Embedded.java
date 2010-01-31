package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This is an Objectify-specific version of the javax.persistence.Embedded annotation.
 * Either will work.</p>
 *
 * The @Embedded annotation on a field instructs Objectify to "unwrap" the object, constructing
 * dotted property names in the datastore entity, representing the object graph.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Embedded {
}
