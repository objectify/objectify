package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Helper which helps take a mapnode's property value and converts it from datastore representation to pojo representation.
 * Note that null checking has already been done.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueTranslator<P, D> extends NullSafeTranslator<P, D>
{
	/** */
	Class<D> datastoreClass;

	/** */
	public ValueTranslator(Class<D> datastoreClass) {
		this.datastoreClass = datastoreClass;
	}

	@Override
	final protected P loadSafe(D value, LoadContext ctx, Path path) throws SkipException {
		if (!datastoreClass.isAssignableFrom(value.getClass()))
			path.throwIllegalState("Expected " + datastoreClass + ", got " + value.getClass() + ": " + value);

		@SuppressWarnings("unchecked")
		D d = (D)value;

		return loadValue(d, ctx, path);
	}

	@Override
	final protected D saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		return saveValue(pojo, index, ctx, path);
	}

	/**
	 * Decode from a property value as stored in the datastore to a type that will be stored in a pojo.
	 *
	 * @param value will not be null, that has already been tested for
	 * @return the format which should be stored in the pojo; a null means store a literal null!
	 * @throws SkipException if this field subtree should be skipped
	 */
	abstract protected P loadValue(D value, LoadContext ctx, Path path) throws SkipException;

	/**
	 * Encode from a normal pojo value to a format that the datastore understands.  Note that a null return value
	 * is a literal instruction to store a null.
	 *
	 * @param value will not be null, that has already been tested for
	 * @return the format which should be stored in the datastore; null means actually store a null!
	 * @throws SkipException if this subtree should be skipped
	 */
	abstract protected D saveValue(P value, boolean index, SaveContext ctx, Path path) throws SkipException;
}
