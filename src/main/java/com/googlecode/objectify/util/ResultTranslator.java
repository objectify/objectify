package com.googlecode.objectify.util;


/**
 * Translates from one arbitrary thing to a Result of another arbitrary thing, caching the value.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultTranslator<F, T> extends ResultCache<T>
{
	private F from;

	public ResultTranslator(F from) {
		this.from = from;
	}

	protected abstract T translate(F from);

	@Override
	public T nowUncached() {
		return translate(from);
	}

	/** We can get away with this because the subclass almost always provides concrete types */
	@Override
	public String toString() {
		if (isExecuted())
			return "ResultTranslator(" + from + " -> " + now() + ")";
		else
			return "ResultTranslator(" + from + ")";
	}
}