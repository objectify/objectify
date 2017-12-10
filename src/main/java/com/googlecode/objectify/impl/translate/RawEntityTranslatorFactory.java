package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle the native datastore FullEntity</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RawEntityTranslatorFactory extends SimpleTranslatorFactory<FullEntity<?>, FullEntity<?>> {

	@SuppressWarnings("unchecked")
	public RawEntityTranslatorFactory() {
		super((Class)FullEntity.class, ValueType.ENTITY);
	}

	@Override
	protected FullEntity<?> toPojo(final Value<FullEntity<?>> value) {
		return value.get();
	}

	@Override
	protected Value<FullEntity<?>> toDatastore(final FullEntity<?> value) {
		return EntityValue.of(value);
	}
}
