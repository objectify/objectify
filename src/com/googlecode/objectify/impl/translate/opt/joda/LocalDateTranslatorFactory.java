package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.reflect.Type;

import org.joda.time.LocalDate;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

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
	protected ValueTranslator<LocalDate, String> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<LocalDate, String>(path, String.class) {
			@Override
			protected LocalDate loadValue(String value, LoadContext ctx) {
				return new LocalDate(value);
			}

			@Override
			protected String saveValue(LocalDate value, SaveContext ctx) {
				return value.toString();
			}
		};
	}
}