package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>When placed on an entity field, the field will be written as a single Blob
 * property using java serialization.</p>
 * 
 * <ul>
 * <li>The field can contain an arbitrary object graph.</li>
 * <li>All classes in the graph must follow Java serialization rules (ie, implement Serializable).</li>
 * <li>You will not be able to use the field or any child fields in queries.</li>
 * <li>Within serialized classes, {@code transient} (the java keyword, not the annotation) fields will not be stored.
 * {@code @Transient} fields *will* be stored!</li>
 * <li>{@code @Serialized} collections <em>can</em> be nested inside {@code @Embedded} collections.</li>
 * <li>Java serialization is opaque to the datastore viewer and other languages (ie gae/python).</li>
 * </ul>
 * 
 * <p>You are <strong>strongly</strong> advised to place {@code serialVersionUID} on all classes
 * that you intend to store as {@code @Serialized}.  Without this, <strong>any</strong> change to your
 * classes will prevent stored objects from being deserialized on fetch.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Serialized
{
}