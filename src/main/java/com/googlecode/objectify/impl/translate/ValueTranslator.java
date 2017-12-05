package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.Path;
import lombok.RequiredArgsConstructor;

/**
 * <p>Does a little bit of expected type checking.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
abstract public class ValueTranslator<P, D> extends NullSafeTranslator<P, D>
{
	private final ValueType expectedValueType;

	@Override
	final protected P loadSafe(final Value<D> value, final LoadContext ctx, final Path path) throws SkipException {
		if (value.getType() != expectedValueType)
			path.throwIllegalState("Expected value type " + expectedValueType + ", got " + value.getType() + ": " + value);

		return loadValue(value, ctx, path);
	}

	@Override
	final protected Value<D> saveSafe(final P pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		return saveValue(pojo, index, ctx, path);
	}

	/**
	 * Decode from a property value as stored in the datastore to a type that will be stored in a pojo.
	 *
	 * @param value will not be null and will actually be the correct type
	 * @return the format which should be stored in the pojo; a null means store a literal null!
	 * @throws SkipException if this field subtree should be skipped
	 */
	abstract protected P loadValue(Value<D> value, LoadContext ctx, Path path) throws SkipException;

	/**
	 * Encode from a normal pojo value to a format that the datastore understands.  Note that a null return value
	 * is a literal instruction to store a null.
	 *
	 * @param value will not be null
	 * @return the format which should be stored in the datastore; null means actually store a null!
	 * @throws SkipException if this subtree should be skipped
	 */
	abstract protected Value<D> saveValue(P value, boolean index, SaveContext ctx, Path path) throws SkipException;
}
