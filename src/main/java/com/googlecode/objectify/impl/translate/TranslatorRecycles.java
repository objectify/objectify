package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.Path;

/**
 * Combines Translator with Recycles, useful so that we can create anonymous classes.
 * Skips if the loaded value is the same as into; this means we won't need to modify potentially
 * final fields.
 *
 * @author Jeff Schnitzer
 */
abstract public class TranslatorRecycles<P, D> implements Translator<P, D>, Recycles {
	@Override
	final public P load(final Value<D> node, final LoadContext ctx, final Path path) throws SkipException {
		@SuppressWarnings("unchecked")
		final P into = (P)ctx.useRecycled();

		final P loaded = loadInto(node, ctx, path, into);

		if (loaded == into)
			throw new SkipException();
		else
			return loaded;
	}

	/**
	 */
	abstract protected P loadInto(Value<D> node, LoadContext ctx, Path path, P into);
}
