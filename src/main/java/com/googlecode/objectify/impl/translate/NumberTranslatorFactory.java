package com.googlecode.objectify.impl.translate;

import com.google.common.primitives.Primitives;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Numbers are funky in the datastore.  You can save numbers of any size, but they always retrieve as Long.
 * For the hell of it, we also handle String in the datastore by trying to parse it.</p>
 *
 * <p>Not a ValueTranslatorFactory because Numbers are not assignable to primitives.  Java lame.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class NumberTranslatorFactory implements TranslatorFactory<Number, Object>
{
	@Override
	public Translator<Number, Object> create(TypeKey<Number> tk, CreateContext ctx, Path path) {
		final Class<?> clazz = Primitives.wrap(tk.getTypeAsClass());

		if (!TypeUtils.isAssignableFrom(Number.class, clazz))
			return null;

		return new ValueTranslator<Number, Object>(Object.class, clazz) {
			@Override
			protected Number loadValue(Object value, LoadContext ctx, Path path) throws SkipException {
				if (value instanceof String) {
					try {
						return coerceNumber(Long.valueOf((String)value), clazz);
					} catch (NumberFormatException ex) {}

					try {
						return coerceNumber(Double.valueOf((String)value), clazz);
					} catch (NumberFormatException ex) {}
				}
				else if (value instanceof Number) {
					return coerceNumber((Number)value, clazz);
				}

				path.throwIllegalState("Don't know how to translate " + value + " to a number");
				return null;	// never gets here
			}

			@Override
			protected Object saveValue(Number value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value;
			}
		};
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long/Double and this screws up
	 * any type that expects something smaller. We don't need to worry about primitive
	 * types because we wrapped the class earlier.
	 */
	private Number coerceNumber(Number value, Class<?> type)
	{
		if (type == Byte.class) return value.byteValue();
		else if (type == Short.class) return value.shortValue();
		else if (type == Integer.class) return value.intValue();
		else if (type == Long.class) return value.longValue();
		else if (type == Float.class) return value.floatValue();
		else if (type == Double.class) return value.doubleValue();
		else throw new IllegalArgumentException();	// should be impossible
	}
}
