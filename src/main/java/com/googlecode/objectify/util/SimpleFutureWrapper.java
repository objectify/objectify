package com.googlecode.objectify.util;

import com.google.appengine.api.utils.FutureWrapper;

import java.util.concurrent.Future;

/**
 * Slightly more convenient than the GAE SDK version.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class SimpleFutureWrapper<K, V> extends FutureWrapper<K, V>
{
	public SimpleFutureWrapper(Future<K> base)
	{
		super(base);
	}

	@Override
	protected Throwable convertException(Throwable cause)
	{
		return cause;
	}
}