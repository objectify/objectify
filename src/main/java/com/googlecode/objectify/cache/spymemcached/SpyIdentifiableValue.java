package com.googlecode.objectify.cache.spymemcached;

import com.googlecode.objectify.cache.IdentifiableValue;
import lombok.Data;
import net.spy.memcached.CASValue;

@Data
public class SpyIdentifiableValue implements IdentifiableValue {
	private final CASValue<Object> casValue;

	@Override
	public Object getValue() {
		return casValue.getValue();
	}
}
