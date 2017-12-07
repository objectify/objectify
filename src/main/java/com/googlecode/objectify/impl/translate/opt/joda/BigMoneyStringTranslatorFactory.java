package com.googlecode.objectify.impl.translate.opt.joda;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.impl.translate.SimpleTranslatorFactory;
import org.joda.money.BigMoney;

/**
 * Stores BigMoney as its string representation.  Note that this does not index properly;
 * you can't safely use inequality filters.  However, indexing is not always necessary
 * and this is a very useful thing to be able to store.
 *
 * <p>All custom translators must be registered *before* entity classes are registered.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BigMoneyStringTranslatorFactory extends SimpleTranslatorFactory<BigMoney, String>
{
	public BigMoneyStringTranslatorFactory() {
		super(BigMoney.class, ValueType.STRING);
	}

	@Override
	protected BigMoney toPojo(final Value<String> value) {
		return BigMoney.parse(value.get());
	}

	@Override
	protected Value<String> toDatastore(final BigMoney value) {
		return StringValue.of(value.toString());
	}
}