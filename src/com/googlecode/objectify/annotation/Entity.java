package com.googlecode.objectify.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>This is an Objectify-specific version of the javax.persistence.Entity annotation.
 * Either will work, but this version will not be picked up by the JDO bytecode
 * enhancer if you are still using it.</p>
 * 
 * <p>Note that the @Entity annotations are not necessary in Objectify; they exist so you can
 * change the datastore kind name.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Entity
{
	String name() default "";
}