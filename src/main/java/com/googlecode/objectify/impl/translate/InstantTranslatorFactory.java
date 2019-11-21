package com.googlecode.objectify.impl.translate;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.time.Instant;

/**
 * <p>Handle java.time.Instant</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class InstantTranslatorFactory extends SimpleTranslatorFactory<Instant, Timestamp> {

	public InstantTranslatorFactory() {
		super(Instant.class, ValueType.TIMESTAMP);
	}

	@Override
	protected Instant toPojo(final Value<Timestamp> value) {
		final Timestamp timestamp = value.get();
		return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());
	}

	@Override
	protected Value<Timestamp> toDatastore(final Instant value) {
		return TimestampValue.of(Timestamp.ofTimeSecondsAndNanos(value.getEpochSecond(), value.getNano()));
	}
}
