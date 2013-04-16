package com.googlecode.objectify.impl.engine;

import com.google.common.base.Function;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Result;

/**
 * Associates a Key<?> with a Result<?> that retuns the value. This is somewhat similar to a Ref<?> except
 * that instead of working "live" with the session cache, it holds the Result<?> directly. This is used
 * for the implementation of queries; since the iterator holds a Result<?> directly, clearing the session
 * won't screw up later
 */
public class KeyResultPair<T> {

	private static final Function<KeyResultPair<?>, Key<?>> KEY_FUNCTION = new Function<KeyResultPair<?>, Key<?>>() {
		@Override
		public Key<?> apply(KeyResultPair<?> input) {
			return input.getKey();
		}
	};
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static <T> Function<KeyResultPair<T>, Key<T>> keyFunction() {
		return (Function)KEY_FUNCTION;
	}

	Key<T> key;
	public Key<T> getKey() { return key; }

	Result<T> result;
	public Result<T> getResult() { return result; }

	/** */
	public KeyResultPair(Key<T> key, Result<T> result) {
		this.key = key;
		this.result = result;
	}
}