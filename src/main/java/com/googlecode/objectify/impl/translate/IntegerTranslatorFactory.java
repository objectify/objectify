package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.LongValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.google.common.primitives.Primitives;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Integers are funky in the datastore.  You can save numbers of any size, but they always retrieve as Long.
 * For the hell of it, we also handle String in the datastore by trying to parse it.</p>
 * 
 * <p>Not a ValueTranslatorFactory because Numbers are not assignable to primitives.  Java lame.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IntegerTranslatorFactory implements TranslatorFactory<Number, Long>
{
	@Override
	public Translator<Number, Long> create(final TypeKey<Number> tk, final CreateContext ctx, final Path path) {
		final Class<?> clazz = Primitives.wrap(tk.getTypeAsClass());

		if (!(clazz == Byte.class || clazz == Short.class || clazz == Integer.class || clazz == Long.class))
			return null;

		return new ValueTranslator<Number, Long>(ValueType.LONG, ValueType.STRING) {
			@Override
			protected Number loadValue(final Value<Long> value, final LoadContext ctx, final Path path) throws SkipException {
				// In theory it's a number, but maybe there was a string instead? We'll have to remove the typecheck
				// in the base ValueTranslator class for this to work.
				if (value.getType() == ValueType.STRING) {
					try {
						return coerceNumber(Long.valueOf(((Value<String>)(Value)value).get()), clazz);
					} catch (NumberFormatException ex) {}
				} else {
					return coerceNumber(value.get(), clazz);
				}

				path.throwIllegalState("Don't know how to translate " + value + " to a number");
				return null;	// never gets here
			}

			@Override
			protected Value<Long> saveValue(final Number value, final SaveContext ctx, final Path path) throws SkipException {
				return LongValue.of(value.longValue());
			}
		};
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long/Double and this screws up
	 * any type that expects something smaller. We don't need to worry about primitive
	 * types because we wrapped the class earlier.
	 */
	private Number coerceNumber(final Number value, final Class<?> type) {
		if (type == Byte.class) return value.byteValue();
		else if (type == Short.class) return value.shortValue();
		else if (type == Integer.class) return value.intValue();
		else if (type == Long.class) return value.longValue();
		else throw new IllegalArgumentException();	// should be impossible
	}
}
