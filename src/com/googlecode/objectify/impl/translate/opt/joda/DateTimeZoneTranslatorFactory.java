package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.reflect.Type;

import org.joda.time.DateTimeZone;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;


/**
 * Stores a joda DateTimeZone as its String id. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DateTimeZoneTranslatorFactory extends ValueTranslatorFactory<DateTimeZone, String>
{
	public DateTimeZoneTranslatorFactory() {
		super(DateTimeZone.class);
	}

	@Override
	protected ValueTranslator<DateTimeZone, String> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<DateTimeZone, String>(path, String.class) {
			@Override
			protected DateTimeZone loadValue(String value, LoadContext ctx) {
				return DateTimeZone.forID(value);
			}

			@Override
			protected String saveValue(DateTimeZone value, SaveContext ctx) {
				return value.getID();
			}
		};
	}
}