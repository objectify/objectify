package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;


/**
 * <p>Translates a byte[] to Blob.  Make sure this translator gets registered *before* the normal ArrayTranslator
 * otherwise it won't get used.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ByteArrayTranslatorFactory extends SimpleTranslatorFactory<byte[], Blob> {

	/** The pojo type this factory recognizes */
	private static final Class<? extends byte[]> BYTE_ARRAY_TYPE = byte[].class;

	/**
	 */
	protected ByteArrayTranslatorFactory() {
		super(BYTE_ARRAY_TYPE, ValueType.BLOB);
	}

	@Override
	protected byte[] toPojo(final Value<Blob> value) {
		return value.get().toByteArray();
	}

	@Override
	protected Value<Blob> toDatastore(final byte[] value) {
		return BlobValue.of(Blob.copyFrom(value));
	}
}
