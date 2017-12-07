package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle the native datastore Key</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RawKeyTranslatorFactory extends SimpleTranslatorFactory<Key, Key> {

	public RawKeyTranslatorFactory() {
		super(Key.class, ValueType.KEY);
	}

	@Override
	protected Key toPojo(final Value<Key> value) {
		return value.get();
	}

	@Override
	protected Value<Key> toDatastore(final Key value) {
		return KeyValue.of(value);
	}
}
