package com.googlecode.objectify.impl.translate.opt.joda;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;
import org.joda.money.Money;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
	protected ValueTranslator<Money, String> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<Money, String>(String.class) {
			@Override
			protected Money loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return Money.parse(value);
			}

			@Override
			protected String saveValue(Money value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}