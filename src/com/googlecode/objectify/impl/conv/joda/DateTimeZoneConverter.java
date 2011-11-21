package com.googlecode.objectify.impl.conv.joda;

import org.joda.time.DateTimeZone;

import com.googlecode.objectify.impl.conv.ConverterCreateContext;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.impl.conv.SimpleConverterFactory;
import com.googlecode.objectify.impl.conv.Converter;


/**
 * Stores a joda DateTimeZone as its String id. 
 */
public class DateTimeZoneConverter extends SimpleConverterFactory<DateTimeZone, String>
{
	public DateTimeZoneConverter() {
		super(DateTimeZone.class);
	}
	
	@Override
	protected Converter<DateTimeZone, String> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<DateTimeZone, String>() {
			@Override
			public String toDatastore(DateTimeZone value, ConverterSaveContext ctx) {
				return value.getID();
			}

			@Override
			public DateTimeZone toPojo(String value, ConverterLoadContext ctx) {
				return DateTimeZone.forID(value);
			}
		};
	}
}