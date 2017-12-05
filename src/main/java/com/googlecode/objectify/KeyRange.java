package com.googlecode.objectify;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import java.util.List;

/**
 * <p>The old SDK had a KeyRange object, which was just a list of keys. The new SDK drops the KeyRange
 * in favor of a simple List. Keeping this for backwards compatibility for now, but you should just use
 * {@code List<Key<T>>} instead.</p>
 *
 * @deprecated Use {@code List<Key<T>>} instead. We will drop this class entirely.
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
@Deprecated
public class KeyRange<T> implements List<Key<T>>
{
	/** */
	@Delegate
	private final List<Key<T>> raw;
}