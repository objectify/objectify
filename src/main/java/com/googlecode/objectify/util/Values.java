package com.googlecode.objectify.util;

import com.google.cloud.datastore.Value;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

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

	/**
	 * The datastore has a weird behavior of reordering values in a list so that indexed ones come before nonindexed
	 * ones. This can really mess up ordered lists. So if we find a heterogeneous list, we need to force index everything.
	 */
	public static void homogenizeIndexes(final List<Value<?>> list) {
		if (isIndexHomogeneous(list))
			return;

		for (int i=0; i<list.size(); i++) {
			final Value<?> value = list.get(i);
			if (value.excludeFromIndexes())
				list.set(i, index(value, true));
		}
	}

	/** @return true if all values have the same index status */
	private static boolean isIndexHomogeneous(final List<Value<?>> list) {
		if (list.isEmpty())
			return true;

		final boolean excluded = list.get(0).excludeFromIndexes();

		for (int i=1; i<list.size(); i++) {
			if (list.get(i).excludeFromIndexes() != excluded)
				return false;
		}

		return true;
	}
}

