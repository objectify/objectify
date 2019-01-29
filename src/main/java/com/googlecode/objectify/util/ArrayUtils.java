package com.googlecode.objectify.util;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class ArrayUtils {

	private ArrayUtils() {
	}

	/**
	 * Copies an array of primitive longs while boxing them at the same time,
	 * and then converting into a list. Can't use {@link Arrays#asList(Object[])}
	 * directly as that will create a list of arrays instead.
	 *
	 * @param elements The array of primitive longs to convert to a list
	 * @return A list of boxed {@link long} values
	 */
	public static List<Long> asList(long... elements) {
		Objects.requireNonNull(elements);
		Long[] copy = new Long[elements.length];
		for (int index = 0; index < elements.length; index++) {
			copy[index] = elements[index];
		}
		return Arrays.asList(copy);
	}
}
