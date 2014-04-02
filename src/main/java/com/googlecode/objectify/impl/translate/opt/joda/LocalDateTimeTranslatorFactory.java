package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.impl.translate.SkipException;
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
	protected ValueTranslator<LocalDateTime, String> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<LocalDateTime, String>(String.class) {
			@Override
			protected LocalDateTime loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return new LocalDateTime(value);
			}

			@Override
			protected String saveValue(LocalDateTime value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}