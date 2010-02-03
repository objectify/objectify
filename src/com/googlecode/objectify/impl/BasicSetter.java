package com.googlecode.objectify.impl;

import java.lang.reflect.Field;

import javax.persistence.Embedded;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>Setter which knows how to set basic leaf values into fields.  This class does
 * not handle collections of any kind; those are handled by subclasses.</p>
 * 
 * <p>The {@code next} value is ignored.</p>
 */
public class BasicSetter extends Setter
{
	/** Need one of these to convert keys */
	ObjectifyFactory factory;
	
	/** The field we set */
	Field field;
	
	/** */
	public BasicSetter(ObjectifyFactory fact, Field field)
	{
		assert !field.isAnnotationPresent(Embedded.class);
		
		this.factory = fact;
		this.field = field;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Setter#set(java.lang.Object, java.lang.Object)
	 */
	@Override
	public void set(Object obj, Object value)
	{
		TypeUtils.field_set(
				this.field,
				obj,
				this.importBasic(value, this.field.getType()));
	}

	/**
	 * Converts a value obtained from the datastore into what gets sent on the field.
	 * The datastore translates values in ways that are not always convenient; for
	 * example, all numbers become Long and booleans become Boolean. This method translates
	 * just the basic types - not collection types.
	 *  
	 * @param fromValue	is the property value that came out of the datastore Entity
	 * @param toType	is the type to convert it to.
	 */
	@SuppressWarnings("unchecked")
	protected Object importBasic(Object fromValue, Class<?> toType)
	{
		if (fromValue == null)
		{
			return null;
		}
		else if (toType.isAssignableFrom(fromValue.getClass()))
		{
			return fromValue;
		}
		else if (toType == String.class)
		{
			if (fromValue instanceof Text)
				return ((Text) fromValue).getValue();
			else
				return fromValue.toString();
		}
		else if (Enum.class.isAssignableFrom(toType))
		{
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>) toType, fromValue.toString());
		}
		else if ((toType == Boolean.TYPE) && (fromValue instanceof Boolean))
		{
			return fromValue;
		}
		else if (fromValue instanceof Number)
		{
			return coerceNumber((Number) fromValue, toType);
		}
		else if (Key.class.isAssignableFrom(toType) && fromValue instanceof com.google.appengine.api.datastore.Key)
		{
			return this.factory.rawKeyToOKey((com.google.appengine.api.datastore.Key) fromValue);
		}

		throw new IllegalArgumentException("Don't know how to convert " + fromValue.getClass() + " to " + toType);
	}

	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	protected Object coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else if (type == String.class) return value.toString();
		else throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}
}
