package com.googlecode.objectify.impl;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.util.FutureHelper;

import java.util.concurrent.Future;

/**
 * Adapts a Future object to a (much more convenient) Result object.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ResultAdapter<T> implements Result<T>
{
	/** Cuts out some typing */
	public static <S> ResultAdapter<S> create(Future<S> fut) {
		return new ResultAdapter<>(fut);
	}

	/** */
	private final Future<T> future;

	/** */
	public ResultAdapter(final Future<T> fut)
	{
		this.future = fut;
	}

	@Override
	public T now() {
		try {
			return this.future.get();
		}
		catch (Exception e) {
			FutureHelper.unwrapAndThrow(e);
			return null;	// make compiler happy
		}
	}
}