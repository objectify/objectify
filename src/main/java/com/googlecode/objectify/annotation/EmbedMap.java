package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation causes Map<String, ?> fields to be stored as an embedded structure similar
 * to how @Embed works; for a field 'someMap', the entity will contain 'someMap.key1', 'someMap.key2', etc.</p>
 *
 * <p>The field must be of type {@code Map} with a key of String.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface EmbedMap
{
}