package com.googlecode.objectify.impl.translate;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.Values;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * <p>Translator that should be extended for typical atomic values. Does a little bit of expected type
 * checking and handles indexing for us.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ValueTranslator<P, D> extends NullSafeTranslator<P, D>
{
	private final ValueType[] expectedValueTypes;

	public ValueTranslator(final ValueType... expectedValueTypes) {
		this.expectedValueTypes = expectedValueTypes;
	}

	private boolean isTypeExpected(final ValueType type) {
		for (final ValueType expectedValueType : expectedValueTypes) {
			if (type == expectedValueType)
				return true;
		}

		return false;
	}

	@Override
	final protected P loadSafe(final Value<D> value, final LoadContext ctx, final Path path) throws SkipException {
		if (!isTypeExpected(value.getType())) {
			// Normally we would just throw an error here but there are some edge cases caused by projection queries.
			// For example, timestamps come back as LongValue and blobs come back as StringValue. We'll special-case them.
			// The downside is that a user who changes a field from 'long' to 'Date' will not trigger an error.
			// The exact logic here comes from com.google.cloud.datastore.ProjectionEntity
			if (value.getType() == ValueType.LONG && isTypeExpected(ValueType.TIMESTAMP)) {
				@SuppressWarnings("unchecked")
				final Value<D> timestampValue = (Value<D>)TimestampValue.of(Timestamp.ofTimeMicroseconds((Long)value.get()));
				return loadValue(timestampValue, ctx, path);
			}
			else if (value.getType() == ValueType.STRING && isTypeExpected(ValueType.BLOB)) {
				@SuppressWarnings("unchecked")
				final Value<D> blobValue = (Value<D>)BlobValue.of(Blob.copyFrom(((String)value.get()).getBytes(StandardCharsets.UTF_8)));
				return loadValue(blobValue, ctx, path);
			}
			else {
				path.throwIllegalState("Expected value of type " + Arrays.toString(expectedValueTypes) + ", got " + value.getType() + ": " + value);
			}
		}

		return loadValue(value, ctx, path);
	}

	@Override
	final protected Value<D> saveSafe(final P pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		return Values.index(saveValue(pojo, ctx, path), index);
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
	abstract protected Value<D> saveValue(P value, SaveContext ctx, Path path) throws SkipException;
}
