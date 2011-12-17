package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.zip.Deflater;

/**
 * <p>When placed on an entity field, the field will be written as a single Blob
 * property using java serialization.  Can also be placed on a class to indicate
 * that all fields of that type should be serialized.</p>
 * 
 * <ul>
 * <li>The field can contain an arbitrary object graph.</li>
 * <li>All classes in the graph must follow Java serialization rules (ie, implement Serializable).</li>
 * <li>You will not be able to use the field or any child fields in queries.</li>
 * <li>Within serialized classes, {@code transient} (the java keyword) fields will not be stored.
 * {@code @Ignore} fields *will* be stored!</li>
 * <li>{@code @Serialize} collections <em>can</em> be nested inside {@code @Embed} collections.</li>
 * <li>Java serialization is opaque to the datastore viewer and other languages (ie gae/python).</li>
 * </ul>
 * 
 * <p>You are <strong>strongly</strong> advised to place {@code serialVersionUID} on all classes
 * that you intend to store as {@code @Serialize}.  Without this, <strong>any</strong> change to your
 * classes will prevent stored objects from being deserialized on fetch.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.FIELD})
public @interface Serialize
{
	/** 
	 * If true, the data stream will be compressed on write using a DeflatorInputStream.  This only affects
	 * writes; on read, Objectify will understand both compressed and uncompressed data. 
	 */
	boolean zip() default false;
	
	/**
	 * If zip is true, sets the compression level of the Deflater.
	 */
	int compressionLevel() default Deflater.DEFAULT_COMPRESSION;
}