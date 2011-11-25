package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>A convenient static method that adds all the joda-money related converters to your factory's conversions.
 * We can't enable the joda-money converters automatically or it would force everyone to add joda-money.jar
 * whether they use it or not.  To enable, call this:</p>
 * 
 * <p>{@code JodaMoneyConverters.add(ObjectifyService.factory());}
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JodaMoneyTranslators
{
	public static void add(ObjectifyFactory fact) {
		fact.getLoaders().add(fact.construct(MoneyStringTranslatorFactory.class));
		fact.getLoaders().add(fact.construct(BigMoneyStringTranslatorFactory.class));
	}
}