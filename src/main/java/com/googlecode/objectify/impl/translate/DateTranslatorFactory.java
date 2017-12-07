package com.googlecode.objectify.impl.translate;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.util.Date;

/**
 * The datastore can't store java.util.Date natively
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class DateTranslatorFactory extends SimpleTranslatorFactory<Date, Timestamp>
{
	/** */
	public DateTranslatorFactory() {
		super(Date.class, ValueType.TIMESTAMP);
	}

	@Override
	protected Date toPojo(final Value<Timestamp> value) {
		return new Date(value.get().toSqlTimestamp().getTime());
	}

	@Override
	protected Value<Timestamp> toDatastore(final Date value) {
		return TimestampValue.of(Timestamp.of(value));
	}
}