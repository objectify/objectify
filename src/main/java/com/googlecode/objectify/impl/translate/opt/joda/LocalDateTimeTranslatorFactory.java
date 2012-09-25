package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.reflect.Type;

import org.joda.time.LocalDateTime;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

/**
 * Stores LocalDateTime as a String in ISO8601 format (yyyy-MM-ddTHH:mm:ss.SSS) 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LocalDateTimeTranslatorFactory extends ValueTranslatorFactory<LocalDateTime, String>
{
	public LocalDateTimeTranslatorFactory() {
		super(LocalDateTime.class);
	}

	@Override
	protected ValueTranslator<LocalDateTime, String> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<LocalDateTime, String>(path, String.class) {
			@Override
			protected LocalDateTime loadValue(String value, LoadContext ctx) {
				return new LocalDateTime(value);
			}

			@Override
			protected String saveValue(LocalDateTime value, SaveContext ctx) {
				return value.toString();
			}
		};
	}
}