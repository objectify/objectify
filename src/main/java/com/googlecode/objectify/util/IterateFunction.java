package com.googlecode.objectify.util;

import java.util.Iterator;

import com.google.common.base.Function;

/**
 * Function that converts Iterables into Iterators.
 */
public class IterateFunction<T> implements Function<Iterable<T>, Iterator<T>>
{
	private static final IterateFunction<Object> INSTANCE = new IterateFunction<Object>();

	@SuppressWarnings("unchecked")
	public static final <T> IterateFunction<T> instance() {
		return (IterateFunction<T>)INSTANCE;
	}

	@Override
	public Iterator<T> apply(Iterable<T> input) {
		return input.iterator();
	}
}
