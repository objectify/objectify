package com.googlecode.objectify.util;

import com.google.common.base.Function;

import java.util.Iterator;

/**
 * Function that converts Iterables into Iterators.
 */
public class IterateFunction<T> implements Function<Iterable<T>, Iterator<T>>
{
	private static final IterateFunction<Object> INSTANCE = new IterateFunction<>();

	@SuppressWarnings("unchecked")
	public static <T> IterateFunction<T> instance() {
		return (IterateFunction<T>)INSTANCE;
	}

	@Override
	public Iterator<T> apply(Iterable<T> input) {
		return input.iterator();
	}
}
