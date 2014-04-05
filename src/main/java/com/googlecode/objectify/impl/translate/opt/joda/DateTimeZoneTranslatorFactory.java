package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.DateTimeZone;


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
	protected ValueTranslator<DateTimeZone, String> createValueTranslator(TypeKey<DateTimeZone> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<DateTimeZone, String>(String.class) {
			@Override
			protected DateTimeZone loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return DateTimeZone.forID(value);
			}

			@Override
			protected String saveValue(DateTimeZone value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.getID();
			}
		};
	}
}