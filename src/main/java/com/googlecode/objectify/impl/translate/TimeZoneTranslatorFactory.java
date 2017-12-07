package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.util.TimeZone;


/**
 * Converts java.util.TimeZone 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class TimeZoneTranslatorFactory extends SimpleTranslatorFactory<TimeZone, String>
{
	public TimeZoneTranslatorFactory() {
		super(TimeZone.class, ValueType.STRING);
	}

	@Override
	protected TimeZone toPojo(final Value<String> value) {
		return TimeZone.getTimeZone(value.get());
	}

	@Override
	protected Value<String> toDatastore(final TimeZone value) {
		return StringValue.of(value.getID());
	}
}