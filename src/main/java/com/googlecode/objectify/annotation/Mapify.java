package com.googlecode.objectify.annotation;

import com.googlecode.objectify.mapper.Mapper;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation causes collection fields to be converted into a map by selecting
 * out a key field of your choosing.</p>
 * 
 * <p>The field must be of type {@code Map}.  An instance of the Mapper will be created and
 * used to extract a key from each value; this will become the key of the map.  The actual
 * value stored in the datastore will be a simple list of the map values.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Mapify
{
	/** 
	 * An instance of this class will be instantiated and used to extract the key from the value. 
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Mapper> value();
}