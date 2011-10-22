package com.googlecode.objectify.impl.conv.joda;

import com.googlecode.objectify.impl.conv.Conversions;

/**
 * <p>A convenient static method that adds all the joda-money related converters to your factory's conversions.
 * We can't enable the joda-money converters automatically or it would force everyone to add joda-money.jar
 * whether they use it or not.  To enable, call this:</p>
 * 
 * <p>{@code JodaMoneyConverters.add(ObjectifyService.factory().getConversions());}
 */
public class JodaMoneyConverters
{
	public static void add(Conversions conv) {
		conv.add(new MoneyStringConverter());
	}
}