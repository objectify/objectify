package com.googlecode.objectify.impl.translate.opt.joda;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
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
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ReadablePartialTranslatorFactory extends ValueTranslatorFactory<ReadablePartial, String>
{
	public ReadablePartialTranslatorFactory() {
		super(ReadablePartial.class);
	}

	@Override
	protected ValueTranslator<ReadablePartial, String> createValueTranslator(final TypeKey<ReadablePartial> tk, final CreateContext ctx, final Path path) {
		final Class<?> clazz = tk.getTypeAsClass();

		// All the Joda partials have a constructor that will accept a String
		final MethodHandle ctor = TypeUtils.getConstructor(clazz, Object.class);

		return new ValueTranslator<ReadablePartial, String>(ValueType.STRING) {
			@Override
			protected ReadablePartial loadValue(final Value<String> value, final LoadContext ctx, final Path path) throws SkipException {
				return TypeUtils.invoke(ctor, value.get());
			}

			@Override
			protected Value<String> saveValue(ReadablePartial value, SaveContext ctx, Path path) throws SkipException {
				return StringValue.of(value.toString());
			}
		};
	}
}