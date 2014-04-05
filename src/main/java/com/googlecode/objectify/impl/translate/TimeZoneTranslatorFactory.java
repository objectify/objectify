package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

import java.util.TimeZone;


/**
 * Converts java.util.TimeZone 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class TimeZoneTranslatorFactory extends ValueTranslatorFactory<TimeZone, String>
{
	public TimeZoneTranslatorFactory() {
		super(TimeZone.class);
	}

	@Override
	protected ValueTranslator<TimeZone, String> createValueTranslator(TypeKey<TimeZone> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<TimeZone, String>(String.class) {
			@Override
			protected TimeZone loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return TimeZone.getTimeZone(value);
			}

			@Override
			protected String saveValue(TimeZone value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.getID();
			}
		};
	}
}