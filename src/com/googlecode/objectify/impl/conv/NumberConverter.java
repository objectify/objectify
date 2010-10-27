package com.googlecode.objectify.impl.conv;


/**
 * <p>Numbers are funky in the datastore.  They have a couple pieces of odd behavior:</p>
 * <ol>
 * <li>You can save numbers of any size.  However, when they are retreived, they will
 * mysteriously all be Long.</li>
 * <li>Numbers suffer from the same primitive type impedance mismatch as Booleans.
 * The type for a primitive int field is Integer.TYPE, which is not assignable
 * with Integer.class.  We need to detect this explicitly.</li> 
 * </ol>
 */
public class NumberConverter implements Converter
{
	@Override
	public Object forDatastore(Object value, ConverterSaveContext ctx)
	{
		if (value instanceof Number)
			return value;
		else
			return null;
	}

	@Override
	public Object forPojo(Object value, Class<?> fieldType, ConverterLoadContext ctx, Object onPojo)
	{
		if (value instanceof Number)
			return coerceNumber((Number)value, fieldType);
		else
			return null;
	}
	
	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	private Object coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else if (type == String.class) return value.toString();
		else return null;	// bailout with null, dunno what to do with it
	}
	
}