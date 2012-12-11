package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.reflect.Type;

import org.joda.time.LocalTime;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

/**
 * Stores LocalTime as a String in ISO8601 format:  HH:MM:SS.ZZZZ
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LocalTimeTranslatorFactory extends ValueTranslatorFactory<LocalTime, String>
{
	public LocalTimeTranslatorFactory() {
		super(LocalTime.class);
	}

	@Override
	protected ValueTranslator<LocalTime, String> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<LocalTime, String>(path, String.class) {
			@Override
			protected LocalTime loadValue(String value, LoadContext ctx) {
				return new LocalTime(value);
			}

			@Override
			protected String saveValue(LocalTime value, SaveContext ctx) {
				return value.toString();
			}
		};
	}
}