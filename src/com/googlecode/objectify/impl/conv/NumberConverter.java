package com.googlecode.objectify.impl.conv;



/**
 * <p>Numbers are funky in the datastore.  They have a couple pieces of odd behavior:</p>
 * 
 * <ol>
 * <li>You can save numbers of any size.  However, when they are retreived, they will
 * mysteriously all be Long.</li>
 * <li>Numbers suffer from the same primitive type impedance mismatch as Booleans.
 * The type for a primitive int field is Integer.TYPE, which is not assignable
 * with Integer.class.  We need to detect this explicitly.</li> 
 * </ol>
 * 
 * <p>We also handle String in the datastore by trying to convert it to a Number.</p>
 */
public class NumberConverter extends SimpleConverterFactory<Number, Object>
{
	/** */
	public NumberConverter() {
		super(Number.class);
	}
	
	/** */
	@Override
	public Converter<Number, Object> create(final Class<?> type, ConverterCreateContext ctx) {
		return new Converter<Number, Object>() {
			
			@Override
			public Number toPojo(Object value, ConverterLoadContext ctx) {
				if (value instanceof String) {
					try {
						return coerceNumber(Long.valueOf((String)value), type);
					} catch (NumberFormatException ex) {} // just pass through
				}
				else if (value instanceof Number) {
					return coerceNumber((Number)value, type);
				}
				
				throw new IllegalStateException(ctx.createErrorMessage("Don't know how to convert " + value + " to a number"));
			}
			
			@Override
			public Number toDatastore(Number value, ConverterSaveContext ctx) {
				return value;
			}
		};
	}
	
	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	private Number coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else throw new IllegalArgumentException();	// should be impossible
	}
}