package com.googlecode.objectify.impl.load.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.time.DateTimeZone;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.load.ValueTranslator;
import com.googlecode.objectify.impl.load.ValueTranslatorFactory;


/**
 * Stores a joda DateTimeZone as its String id. 
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class DateTimeZoneTranslatorFactory extends ValueTranslatorFactory<DateTimeZone, String>
{
	public DateTimeZoneTranslatorFactory() {
		super(DateTimeZone.class);
	}

	@Override
	protected ValueTranslator<DateTimeZone, String> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type)
	{
		return new ValueTranslator<DateTimeZone, String>(path, String.class) {
			@Override
			protected DateTimeZone loadValue(String value, LoadContext ctx) {
				return DateTimeZone.forID(value);
			}

			@Override
			protected String saveValue(DateTimeZone value, SaveContext ctx) {
				return value.getID();
			}
		};
	}
}