package com.googlecode.objectify.impl.conv;

import java.util.TimeZone;


/**
 * Converts java.util.TimeZone 
 */
public class TimeZoneConverter extends SimpleConverterFactory<TimeZone, String>
{
	public TimeZoneConverter() {
		super(TimeZone.class);
	}
	
	@Override
	protected Converter<TimeZone, String> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<TimeZone, String>() {
			
			@Override
			public TimeZone toPojo(String value, ConverterLoadContext ctx) {
				return TimeZone.getTimeZone(value);
			}
			
			@Override
			public String toDatastore(TimeZone value, ConverterSaveContext ctx) {
				return value.getID();
			}
		};
	}
}