package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

import java.util.Collection;

/**
 * Combines Translator with UsesExistingValue, useful so that we can create anonymous classes.
 *
 * @author Jeff Schnitzer
 */
abstract public class TranslatorUsesExistingValue<P, D> implements Translator<P, D>, UsesExistingValue {
	@Override
	final public P load(D node, LoadContext ctx, Path path) throws SkipException {
		P existing = (P)ctx.getExistingValue();
		P loaded = load(node, ctx, path, existing);

		if (loaded == existing)
			throw new SkipException();
		else
			return loaded;
	}

	/**
	 * @param existingValue will be the existing value in the POJO for the relevant path. Possibly null.
	 */
	abstract protected P load(D node, LoadContext ctx, Path path, P existingValue);
}
