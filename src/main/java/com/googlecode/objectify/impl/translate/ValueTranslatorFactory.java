package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

/**
 * Provides a little boilerplate for translators that work on simple atomic types.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueTranslatorFactory<P, D> implements TranslatorFactory<P, D>
{
	/** */
	Class<? extends P> pojoType;
	
	/** */
	protected ValueTranslatorFactory(Class<? extends P> pojoType) {
		this.pojoType = pojoType;
	}

	@Override
	final public Translator<P, D> create(TypeKey<P> tk, CreateContext ctx, Path path) {
		if (this.pojoType.isAssignableFrom(GenericTypeReflector.erase(tk.getType()))) {
			return createValueTranslator(tk, ctx, path);
		} else {
			return null;
		}
	}

	/**
	 * Create a translator, knowing that we have the appropriate type.  You don't need to check for type matching.
	 * @param tk type is guaranteed to erase to something assignable to Class<P>
	 */
	abstract protected ValueTranslator<P, D> createValueTranslator(TypeKey<P> tk, CreateContext ctx, Path path);
}