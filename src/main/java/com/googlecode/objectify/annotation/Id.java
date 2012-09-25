package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Place this annotation on a single Long, long, or String field of an entity POJO.  This field defines the
 * id of the entity, which is one part of the key.  The entity itself is uniquely identified by its (optional)
 * parent, kind (typically the class), and the id.</p>
 * 
 * <p>If your entity has a @Parent, the id will not be globally unique!  Ids are only unique for a particular
 * parent (and kind).</p> 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Id
{
}