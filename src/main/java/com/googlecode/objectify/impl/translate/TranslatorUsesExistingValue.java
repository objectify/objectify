package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

/**
 * Combines Translator with UsesExistingValue, useful so that we can create anonymous classes.
 * Skips if the loaded value is the same as into; this means we won't need to modify potentially
 * final fields.
 *
 * @author Jeff Schnitzer
 */
abstract public class TranslatorUsesExistingValue<P, D> implements Translator<P, D>, UsesExistingValue {
	@Override
	final public P load(D node, LoadContext ctx, Path path, P into) throws SkipException {
		P loaded = load(node, ctx, path, into);

		if (loaded == into)
			throw new SkipException();
		else
			return loaded;
	}

	/**
	 */
	abstract protected P loadInto(D node, LoadContext ctx, Path path, P into);
}
