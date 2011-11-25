package com.googlecode.objectify.impl.load.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.money.Money;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.load.ValueTranslator;
import com.googlecode.objectify.impl.load.ValueTranslatorFactory;

/**
 * Stores Money as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MoneyStringTranslatorFactory extends ValueTranslatorFactory<Money, String>
{
	public MoneyStringTranslatorFactory() {
		super(Money.class);
	}
	
	@Override
	protected ValueTranslator<Money, String> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type)
	{
		return new ValueTranslator<Money, String>(path, String.class) {
			@Override
			protected Money loadValue(String value, LoadContext ctx) {
				return Money.parse(value);
			}

			@Override
			protected String saveValue(Money value, SaveContext ctx) {
				return value.toString();
			}
		};
	}
}