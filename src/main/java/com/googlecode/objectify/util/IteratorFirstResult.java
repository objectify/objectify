package com.googlecode.objectify.util;

import java.util.Iterator;



/**
 * Extracts the first value of the iterator as the result value. If the iterator has no first value,
 * the result value is null.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class IteratorFirstResult<T> extends ResultCache<T>
{
	private static final long serialVersionUID = 1L;

	/**
	 */
	Iterator<T> iterator;

	/**
	 */
	public IteratorFirstResult(Iterator<T> iterator) {
		this.iterator = iterator;
	}

	/**
	 */
	@Override
	protected T nowUncached() {
		if (iterator.hasNext())
			return iterator.next();
		else
			return null;
	}
}