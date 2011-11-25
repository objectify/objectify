package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

/**
 * Provides a little boilerplate for translators that work on simple atomic types. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueTranslatorFactory<P, D> implements TranslatorFactory<P>
{
	/** */
	Class<? extends P> pojoType;
	
	/** */
	protected ValueTranslatorFactory(Class<? extends P> pojoType) {
		this.pojoType = pojoType;
	}

	@Override
	final public Translator<P> create(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx) {
		if (this.pojoType.isAssignableFrom(GenericTypeReflector.erase(type))) {
			return createSafe(path, fieldAnnotations, type, ctx);
		} else {
			return null;
		}
	}

	/**
	 * Create a translator, knowing that we have the appropriate type.  You don't need to check for type matching.
	 * @param type is guaranteed to erase to something assignable to Class<P>
	 */
	abstract protected ValueTranslator<P, D> createSafe(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx);
}