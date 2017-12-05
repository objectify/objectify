package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.google.common.primitives.Primitives;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Watches out for RawValue and performs the necessary conversion if we get one. This will happen
 * during projection queries.</p>
 *
 * TODO: deleteme
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Deprecated
abstract public class ProjectionSafeTranslator<P, D> extends NullSafeTranslator<P, D>
{
	/** The class in the datastore that we project to */
	private final Class<? extends D> projectionClass;

	/** */
	public ProjectionSafeTranslator(final Class<? extends D> projectionClass) {
		// Projection fails if we do not use the wrapper type
		this.projectionClass = Primitives.wrap(projectionClass);
	}

	@Override
	final protected P loadSafe(Value<D> value, LoadContext ctx, Path path) throws SkipException {
		// Projection queries produce RawValue because the index data is not self-describing.
		// Here we have the expected datastore type, so we can obtain it right away.
//		if (value instanceof RawValue) {
//			value = (D)((RawValue)value).asType(projectionClass);
//
//			// Annoyingly, null values still come back as a RawValue but the content is null
//			if (value == null)
//				return null;
//		}

		return loadSafe2(value, ctx, path);
	}

	/**
	 * Decode from a property value as stored in the datastore to a type that will be stored in a pojo.
	 *
	 * @param value will not be null and will not be RawValue
	 * @return the format which should be stored in the pojo; a null means store a literal null!
	 * @throws com.googlecode.objectify.impl.translate.SkipException if this field subtree should be skipped
	 */
	abstract protected P loadSafe2(Value<D> value, LoadContext ctx, Path path) throws SkipException;
}
