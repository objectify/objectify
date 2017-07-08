package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.zip.Deflater;

/**
 * <p>When placed on an entity field, the field will be written as a single Blob
 * property using protobuf serialization. </p>
 * 
 * <ul>
 * <li>The field must contain a Google Protocol Buffer object.</li>
 * <li>You will not be able to use the field or any child fields in queries.
 *     (Support could be added in the future.)</li>
 * </ul>
 *
 * @author Ivan Jager <aij+@mrph.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Protobuf
{
	// TODO: Will we need to add anything here for deserialization to be possible?
}
