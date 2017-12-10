package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import lombok.extern.slf4j.Slf4j;


/**
 * Knows how to convert Strings. Thankfully, string handling in the new cloud SDK is much simpler;
 * there is no longer a Text type.
 */
@Slf4j
public class StringTranslatorFactory extends SimpleTranslatorFactory<String, String>
{
	/** */
	public StringTranslatorFactory() {
		super(String.class, ValueType.STRING, ValueType.LONG, ValueType.DOUBLE, ValueType.BOOLEAN);
	}

	@Override
	protected String toPojo(final Value<String> value) {
		// Careful - if you inline this variable, the compiler will optimize out the toString() and we'll throw CCE
		final Object thing = value.get();
		return thing.toString();
	}

	@Override
	protected Value<String> toDatastore(final String value) {
		return StringValue.of(value);
	}
}