package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.KeyValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.Key;


/**
 * Knows how to convert Key<?> objects to datastore-native Key objects and vice-versa.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyTranslatorFactory extends SimpleTranslatorFactory<Key<?>, com.google.cloud.datastore.Key>
{
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public KeyTranslatorFactory() {
		super((Class)Key.class, ValueType.KEY);
	}

	@Override
	protected Key<?> toPojo(final Value<com.google.cloud.datastore.Key> value) {
		return Key.create(value.get());
	}

	@Override
	protected Value<com.google.cloud.datastore.Key> toDatastore(final Key<?> value) {
		return KeyValue.of(value.getRaw());
	}
}