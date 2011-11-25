package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.joda.money.Money;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

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
	protected ValueTranslator<Money, String> createSafe(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx)
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