package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>A convenient static method that adds all the joda-money related converters to your factory's conversions.
 * We can't enable the joda-money converters automatically or it would force everyone to add joda-money.jar
 * whether they use it or not.  To enable, call this:</p>
 * 
 * <p>{@code JodaMoneyConverters.add(ObjectifyService.factory());}
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JodaMoneyTranslators
{
	private JodaMoneyTranslators() {
	}

	public static void add(ObjectifyFactory fact) {
		fact.getTranslators().add(new MoneyStringTranslatorFactory());
		fact.getTranslators().add(new BigMoneyStringTranslatorFactory());
	}
}