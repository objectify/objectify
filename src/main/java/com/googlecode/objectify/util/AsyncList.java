package com.googlecode.objectify.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;


/**
 * A List which wraps an iterable and asynchronously provides content. No methods are called on the wrapped
 * iterable until client calls are made.
 */
public class AsyncList<T> extends AbstractList<T>
{
	Iterable<T> wrapped;
	List<T> actual;

	public AsyncList(Iterable<T> wrapped) {
		this.wrapped = wrapped;
	}

	private List<T> getActual() {
		if (actual == null) {
			actual = new ArrayList<T>();
			for (T t: wrapped)
				actual.add(t);
		}

		return actual;
	}

	@Override
	public T get(int index) {
		return getActual().get(index);
	}

	@Override
	public int size() {
		return getActual().size();
	}

}
