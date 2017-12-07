package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.BooleanValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle Boolean and boolean types</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class BooleanTranslatorFactory extends SimpleTranslatorFactory<Boolean, Boolean> {

	public BooleanTranslatorFactory() {
		super(Boolean.class, ValueType.BOOLEAN);
	}

	@Override
	protected Boolean toPojo(final Value<Boolean> value) {
		return value.get();
	}

	@Override
	protected Value<Boolean> toDatastore(final Boolean value) {
		return BooleanValue.of(value);
	}
}
