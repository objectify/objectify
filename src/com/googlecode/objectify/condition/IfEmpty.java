package com.googlecode.objectify.condition;

import java.util.Collection;
import java.util.Map;


/**
 * <p>Simple If condition that returns true if the value is null or empty.
 * The value can be one of:</p>
 * <ul>
 * <li>java.lang.String</li>
 * <li>java.util.Collection</li>
 * <li>java.util.Map</li>
 * </ul>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IfEmpty extends ValueIf<Object>
{
	@Override
	public boolean matches(Object value)
	{
		if (value == null)
			return true;
		
		if (value instanceof String)
			return ((String)value).isEmpty();
		
		if (value instanceof Collection<?>)
			return ((Collection<?>)value).isEmpty();
		
		if (value instanceof Map<?, ?>)
			return ((Map<?, ?>)value).isEmpty();

		throw new IllegalArgumentException("Don't know what to do with something of type " + value.getClass().getName());
	}
}