package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.LocalDate;

/**
 * Stores LocalDate as a String in ISO format:  yyyy-MM-dd 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LocalDateTranslatorFactory extends ValueTranslatorFactory<LocalDate, String>
{
	public LocalDateTranslatorFactory() {
		super(LocalDate.class);
	}

	@Override
	protected ValueTranslator<LocalDate, String> createValueTranslator(TypeKey<LocalDate> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<LocalDate, String>(String.class) {
			@Override
			protected LocalDate loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return new LocalDate(value);
			}

			@Override
			protected String saveValue(LocalDate value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}