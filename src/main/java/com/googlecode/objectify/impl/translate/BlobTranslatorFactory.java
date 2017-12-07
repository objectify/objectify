package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle the native datastore Blob</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BlobTranslatorFactory extends SimpleTranslatorFactory<Blob, Blob> {

	public BlobTranslatorFactory() {
		super(Blob.class, ValueType.BLOB);
	}

	@Override
	protected Blob toPojo(final Value<Blob> value) {
		return value.get();
	}

	@Override
	protected Value<Blob> toDatastore(final Blob value) {
		return BlobValue.of(value);
	}
}
