package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;
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
	final public Translator<P> create(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		if (this.pojoType.isAssignableFrom(GenericTypeReflector.erase(type))) {
			return create(fact, path, fieldAnnotations, type);
		} else {
			return null;
		}
	}

	/**
	 * Create a translator, knowing that we have the appropriate type.  You don't need to check for type matching.
	 * @param type is guaranteed to erase to something assignable to Class<P>
	 */
	abstract protected ValueTranslator<P, D> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type);
}