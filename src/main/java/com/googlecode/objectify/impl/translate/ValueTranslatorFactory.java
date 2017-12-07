package com.googlecode.objectify.impl.translate;

import com.google.common.primitives.Primitives;
import com.googlecode.objectify.impl.Path;
import lombok.RequiredArgsConstructor;

/**
 * Provides a little boilerplate for translators that work on simple atomic types. Automatically
 * wraps primitives so we don't have to worry about them. Handles indexing for us.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
abstract public class ValueTranslatorFactory<P, D> implements TranslatorFactory<P, D>
{
	/** */
	private final Class<? extends P> pojoType;
	
	@Override
	final public Translator<P, D> create(final TypeKey<P> tk, final CreateContext ctx, final Path path) {
		final Class<P> clazz = Primitives.wrap(tk.getTypeAsClass());

		if (this.pojoType.isAssignableFrom(clazz)) {
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