package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.DoubleValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.google.common.primitives.Primitives;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Floats are funky in the datastore.  You can save numbers of any size, but they always retrieve as Double.
 * For the hell of it, we also handle String in the datastore by trying to parse it.</p>
 * 
 * <p>Not a ValueTranslatorFactory because Numbers are not assignable to primitives.  Java lame.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class FloatTranslatorFactory implements TranslatorFactory<Number, Double>
{
	@Override
	public Translator<Number, Double> create(final TypeKey<Number> tk, final CreateContext ctx, final Path path) {
		final Class<?> clazz = Primitives.wrap(tk.getTypeAsClass());

		if (!(clazz == Float.class || clazz == Double.class))
			return null;

		return new ValueTranslator<Number, Double>(ValueType.DOUBLE, ValueType.STRING) {
			@Override
			protected Number loadValue(final Value<Double> value, final LoadContext ctx, final Path path) throws SkipException {
				if (value.getType() == ValueType.STRING) {
					try {
						return coerceNumber(Double.valueOf(((Value<String>)(Value)value).get()), clazz);
					} catch (NumberFormatException ex) {}
				}
				else {
					return coerceNumber(value.get(), clazz);
				}

				path.throwIllegalState("Don't know how to translate " + value + " to a number");
				return null;	// never gets here
			}

			@Override
			protected Value<Double> saveValue(final Number value, final SaveContext ctx, final Path path) throws SkipException {
				return DoubleValue.of(value.doubleValue());
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
		if (type == Float.class) return value.floatValue();
		else if (type == Double.class) return value.doubleValue();
		else throw new IllegalArgumentException();	// should be impossible
	}
}
