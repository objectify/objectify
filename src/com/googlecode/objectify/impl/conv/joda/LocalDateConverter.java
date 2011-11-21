package com.googlecode.objectify.impl.conv.joda;

import org.joda.time.LocalDate;

import com.googlecode.objectify.impl.conv.ConverterCreateContext;
import com.googlecode.objectify.impl.conv.ConverterLoadContext;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;
import com.googlecode.objectify.impl.conv.SimpleConverterFactory;
import com.googlecode.objectify.impl.conv.Converter;


/**
 * Stores LocalDate as a String in ISO format:  yyyy-MM-dd 
 */
public class LocalDateConverter extends SimpleConverterFactory<LocalDate, String>
{
	public LocalDateConverter() {
		super(LocalDate.class);
	}
	
	@Override
	protected Converter<LocalDate, String> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<LocalDate, String>() {

			@Override
			public String toDatastore(LocalDate value, ConverterSaveContext ctx) {
				return value.toString();
			}

			@Override
			public LocalDate toPojo(String value, ConverterLoadContext ctx) {
				return new LocalDate(value);
			}
		};
	}
}