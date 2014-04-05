package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.LocalDateTime;

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
	protected ValueTranslator<LocalDateTime, String> createValueTranslator(TypeKey<LocalDateTime> tk, CreateContext ctx, Path path) {
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