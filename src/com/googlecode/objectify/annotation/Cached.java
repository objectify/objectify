package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Place this on any POJO entity class to cause it to be cached in the memcache.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Cached
{
	/**
	 * Number of seconds after which the cached copy should be expired; the default value (0) is
	 * "keep as long as possible".  This is not a guarantee; the memcache can be wiped at any
	 * time due to memory pressure or the whim of Google's operations team.
	 */
	int expirationSeconds() default 0;
}