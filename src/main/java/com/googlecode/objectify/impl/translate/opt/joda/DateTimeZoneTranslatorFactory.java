package com.googlecode.objectify.impl.translate.opt.joda;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;
import org.joda.time.DateTimeZone;


/**
 * Stores a joda DateTimeZone as its String id.
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DateTimeZoneTranslatorFactory extends SimpleTranslatorFactory<DateTimeZone, String>
{
	public DateTimeZoneTranslatorFactory() {
		super(DateTimeZone.class, ValueType.STRING);
	}

	@Override
	protected DateTimeZone toPojo(final Value<String> value) {
		return DateTimeZone.forID(value.get());
	}

	@Override
	protected Value<String> toDatastore(final DateTimeZone value) {
		return StringValue.of(value.getID());
	}
}