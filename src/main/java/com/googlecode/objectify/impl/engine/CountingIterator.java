package com.googlecode.objectify.impl.engine;

import java.util.Iterator;

/**
 */
public class CountingIterator<T> implements Iterator<T> {

	Iterator<T> base;
	int count;

	public CountingIterator(Iterator<T> base) {
		this.base = base;
	}

	public int getCount() { return count; }

	@Override
	public boolean hasNext() {
		return base.hasNext();
	}

	@Override
	public T next() {
		count++;
		return base.next();
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}