package com.googlecode.objectify.util;

import com.google.common.base.Function;
import com.googlecode.objectify.Result;

/**
 * Simple function that extracts the value of a Result<?>
 */
public class ResultNowFunction<T> implements Function<Result<T>, T>
{
	private static final ResultNowFunction<Object> INSTANCE = new ResultNowFunction<>();

	@SuppressWarnings("unchecked")
	public static <T> ResultNowFunction<T> instance() {
		return (ResultNowFunction<T>)INSTANCE;
	}

	@Override
	public T apply(Result<T> input) {
		return input.now();
	}
}
