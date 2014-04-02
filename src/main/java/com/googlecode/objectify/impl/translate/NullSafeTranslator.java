package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

/**
 * <p>Handles null checking so we don't have to do it everywhere.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class NullSafeTranslator<P, D> implements Translator<P, D>
{
	@Override
	final public P load(D node, LoadContext ctx, Path path, P into) throws SkipException {
		if (node == null)
			return null;
		else
			return loadSafe(node, ctx, path, into);
	}

	@Override
	final public D save(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		if (pojo == null)
			return null;
		else
			return saveSafe(pojo, index, ctx, path);
	}

	/**
	 * Implement this, returning a proper translated value
	 * @param node will never be null
	 */
	abstract protected P loadSafe(D node, LoadContext ctx, Path path, P into) throws SkipException;
	
	/**
	 * Implement this, returning a proper translated value
	 * @param pojo will never be null
	 */
	abstract protected D saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException;
}
