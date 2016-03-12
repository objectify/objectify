package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.ObjectifyFactory;

/**
 * <p>A convenient static method that adds all the joda-time related converters to your factory's conversions.
 * We can't enable the joda-time converters automatically or it would force everyone to add joda-time.jar
 * whether they use it or not.  To enable, call this:</p>
 *
 * <p>{@code JodaTimeTranslators.add(ObjectifyService.factory());}
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class JodaTimeTranslators
{
	private JodaTimeTranslators() {
	}

	public static void add(ObjectifyFactory fact) {
		fact.getTranslators().add(new ReadableInstantTranslatorFactory());
		fact.getTranslators().add(new ReadablePartialTranslatorFactory());
		fact.getTranslators().add(new DateTimeZoneTranslatorFactory());
	}
}