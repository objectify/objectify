package com.googlecode.objectify.impl.translate.opt.joda;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.impl.translate.SkipException;
import org.joda.money.BigMoney;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.ValueTranslator;
import com.googlecode.objectify.impl.translate.ValueTranslatorFactory;

/**
 * Stores BigMoney as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BigMoneyStringTranslatorFactory extends ValueTranslatorFactory<BigMoney, String>
{
	public BigMoneyStringTranslatorFactory() {
		super(BigMoney.class);
	}
	
	@Override
	protected ValueTranslator<BigMoney, String> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<BigMoney, String>(String.class) {
			@Override
			protected BigMoney loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				return BigMoney.parse(value);
			}

			@Override
			protected String saveValue(BigMoney value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}