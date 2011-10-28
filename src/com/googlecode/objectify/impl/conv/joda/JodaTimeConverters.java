package com.googlecode.objectify.impl.conv.joda;

import com.googlecode.objectify.impl.conv.Conversions;

/**
 * <p>A convenient static method that adds all the joda-time related converters to your factory's conversions.
 * We can't enable the joda-time converters automatically or it would force everyone to add joda-time.jar
 * whether they use it or not.  To enable, call this:</p>
 * 
 * <p>{@code JodaTimeConverters.add(ObjectifyService.factory().getConversions());}
 */
public class JodaTimeConverters
{
	public static void add(Conversions conv) {
		conv.add(new ReadableInstantConverter());
		conv.add(new LocalDateConverter());
		conv.add(new DateTimeZoneConverter());
	}
}