package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

/**
 * Wraps a Result, translating from one type to another and caching the result
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultWrapper<F, T> extends ResultTranslator<Result<F>, T>
{
	public ResultWrapper(final Result<F> base) {
		super(base);
	}

	protected abstract T wrap(F orig);

	@Override
	final protected T translate(final Result<F> from) {
		return wrap(from.now());
	}
}