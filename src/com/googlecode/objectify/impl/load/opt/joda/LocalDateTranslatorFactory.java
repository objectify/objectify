package com.googlecode.objectify.impl.load.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.time.LocalDate;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.load.ValueTranslator;
import com.googlecode.objectify.impl.load.ValueTranslatorFactory;

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
	protected ValueTranslator<LocalDate, String> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type)
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