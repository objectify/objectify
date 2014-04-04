package com.googlecode.objectify.annotation;

import com.googlecode.objectify.impl.translate.TranslatorFactory;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <p>Lets you define a particular translator for a specific property (field or @AlsoLoad method).
 * The translator factory must produce a translator when handed the property on which this annotation
 * is placed.</p>
 * 
 * <p>"Early" translator factories are executed before collection translator factories and therefore can manipulate
 * the whole collection value.  Late translator factories are only responsible for translating the contents of
 * a collection.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface Translate
{
	/**
	 * Factory class which will be applied to this field.  It will be constructed with
	 * ObjectifyFactory.construct().  It must produce a Translator for the field.
	 */
	Class<? extends TranslatorFactory<?, ?>> value();
	
	/**
	 * If true, the factory will be placed early in the chain, before collection translators.  This
	 * will let you explicitly translate collection fields rather than collection contents,
	 * which is the default.
	 */
	boolean early() default false;
}