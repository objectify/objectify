package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.LatLng;
import com.google.cloud.datastore.LatLngValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

/**
 * <p>Handle the native datastore LatLng</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LatLngTranslatorFactory extends SimpleTranslatorFactory<LatLng, LatLng> {

	public LatLngTranslatorFactory() {
		super(LatLng.class, ValueType.LAT_LNG);
	}

	@Override
	protected LatLng toPojo(final Value<LatLng> value) {
		return value.get();
	}

	@Override
	protected Value<LatLng> toDatastore(final LatLng value) {
		return LatLngValue.of(value);
	}
}
