package com.googlecode.objectify.util;

import com.google.cloud.datastore.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * Some static utility methods for interacting with {@code Value<?>}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Values
{
	/** @return a value that has the specified index flag (possibly the original itself) */
	@SuppressWarnings("unchecked")
	public static <D> Value<D> index(final Value<D> original, final boolean index) {
		if (original.excludeFromIndexes() == !index) {
			return original;
		} else {
			return (Value<D>)original.toBuilder().setExcludeFromIndexes(!index).build();
		}
	}
}

