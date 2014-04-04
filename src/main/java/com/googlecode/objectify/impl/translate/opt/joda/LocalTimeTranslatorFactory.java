package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.LocalTime;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
	protected ValueTranslator<LocalTime, String> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<LocalTime, String>(String.class) {
			@Override
			protected LocalTime loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return new LocalTime(value);
			}

			@Override
			protected String saveValue(LocalTime value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}