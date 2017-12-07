package com.googlecode.objectify.impl.translate;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle the native datastore Timestamp</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class TimestampTranslatorFactory extends SimpleTranslatorFactory<Timestamp, Timestamp> {

	public TimestampTranslatorFactory() {
		super(Timestamp.class, ValueType.TIMESTAMP);
	}

	@Override
	protected Timestamp toPojo(final Value<Timestamp> value) {
		return value.get();
	}

	@Override
	protected Value<Timestamp> toDatastore(final Timestamp value) {
		return TimestampValue.of(value);
	}
}
