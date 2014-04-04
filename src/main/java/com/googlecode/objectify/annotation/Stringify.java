package com.googlecode.objectify.annotation;

import com.googlecode.objectify.stringifier.Stringifier;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This annotation establishes a Stringifier for a field. This is used for embedded
 * Map structures that require something other than a String key; for example, you may
 * want to have a Map<Long, Thing>. A Stringifier can convert the Long to the required String
 * key type.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface Stringify
{
	/** 
	 * An instance of this class will be instantiated and used to convert objects to and from String.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Stringifier> value();
}