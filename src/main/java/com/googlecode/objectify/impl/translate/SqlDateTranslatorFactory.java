package com.googlecode.objectify.impl.translate;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.TimestampValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.sql.Date;

/**
 * The datastore can't store java.sql.Date natively
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class SqlDateTranslatorFactory extends SimpleTranslatorFactory<Date, Timestamp>
{
	/** */
	public SqlDateTranslatorFactory() {
		super(java.sql.Date.class, ValueType.TIMESTAMP);
	}

	@Override
	protected Date toPojo(final Value<Timestamp> value) {
		return new java.sql.Date(value.get().toSqlTimestamp().getTime());
	}

	@Override
	protected Value<Timestamp> toDatastore(final Date value) {
		return TimestampValue.of(Timestamp.of(value));
	}
}