package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.TypeKey;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.time.ReadablePartial;

import java.lang.invoke.MethodHandle;


/**
 * Converts Joda ReadablePartials (LocalDate, LocalDateTime, YearMonth, etc) into String (ISO-8601) representation
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ReadablePartialTranslatorFactory extends ValueTranslatorFactory<ReadablePartial, String>
{
	public ReadablePartialTranslatorFactory() {
		super(ReadablePartial.class);
	}

	@Override
	protected ValueTranslator<ReadablePartial, String> createValueTranslator(TypeKey<ReadablePartial> tk, CreateContext ctx, Path path) {
		final Class<?> clazz = tk.getTypeAsClass();

		// All the Joda partials have a constructor that will accept a String
		final MethodHandle ctor = TypeUtils.getConstructor(clazz, Object.class);

		return new ValueTranslator<ReadablePartial, String>(String.class) {
			@Override
			protected ReadablePartial loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return TypeUtils.invoke(ctor, value);
			}

			@Override
			protected String saveValue(ReadablePartial value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}