package com.googlecode.objectify.impl.conv.joda;

import java.lang.reflect.Constructor;
import java.util.Date;

import org.joda.time.ReadableInstant;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.ConverterCreateContext;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.impl.conv.SimpleConverterFactory;
import com.googlecode.objectify.impl.conv.Converter;


/**
 * Converts Joda ReadableInstants (DateTime, DateMidnight, etc) into java.util.Date 
 */
public class ReadableInstantConverter extends SimpleConverterFactory<ReadableInstant, Date>
{
	public ReadableInstantConverter() {
		super(ReadableInstant.class);
	}
	
	@Override
	protected Converter<ReadableInstant, Date> create(final Class<?> type, ConverterCreateContext ctx) {
		return new Converter<ReadableInstant, Date>() {

			@Override
			public Date toDatastore(ReadableInstant value, ConverterSaveContext ctx) {
				return value.toInstant().toDate();
			}

			@Override
			public ReadableInstant toPojo(Date value, ConverterLoadContext ctx) {
				// All the Joda instants have a constructor that will take a Date
				Constructor<?> ctor = TypeUtils.getConstructor(type, Object.class);
				return (ReadableInstant)TypeUtils.newInstance(ctor, value);
			}
		};
	}
}