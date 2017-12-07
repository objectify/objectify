package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.NullValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Handles null checking so we don't have to do it everywhere. Handles NullValue
 * where appropriate.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class NullSafeTranslator<P, D> implements Translator<P, D>
{
	@Override
	final public P load(final Value<D> node, final LoadContext ctx, final Path path) throws SkipException {
		if (node == null || node.getType() == ValueType.NULL)
			return null;
		else
			return loadSafe(node, ctx, path);
	}

	@SuppressWarnings("unchecked")
	@Override
	final public Value<D> save(final P pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		if (pojo == null)
			// Watch out for indexing this
			return (Value<D>)NullValue.newBuilder().setExcludeFromIndexes(!index).build();
		else
			return saveSafe(pojo, index, ctx, path);
	}

	/**
	 * Implement this, returning a proper translated value
	 * @param node will never be null or NullValue
	 */
	abstract protected P loadSafe(Value<D> node, LoadContext ctx, Path path) throws SkipException;
	
	/**
	 * Implement this, returning a proper translated value
	 * @param pojo will never be null
	 */
	abstract protected Value<D> saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException;
}
