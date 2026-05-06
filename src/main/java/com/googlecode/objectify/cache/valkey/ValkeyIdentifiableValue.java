package com.googlecode.objectify.cache.valkey;

import com.googlecode.objectify.cache.IdentifiableValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * CAS handle for {@link ValkeyCacheService}. Wraps the deserialized value alongside the raw bytes
 * read from Valkey, which are later passed to {@code SET ... IFEQ comparison-value} so the server
 * can atomically reject the write when another client has changed the key in the meantime.
 */
@RequiredArgsConstructor
public class ValkeyIdentifiableValue implements IdentifiableValue {

	@Getter
	private final Object value;

	/** Raw bytes that were stored at the time of the read; used as the IFEQ comparison value. */
	@Getter
	private final byte[] rawBytes;
}
