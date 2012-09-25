package com.googlecode.objectify.cache;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


/**
 * A Future which merges some previously loaded values with the results of another
 * Future that is in progress.  It can apply to any key/value pair type; typically
 * it will be for Key/Entity or Key<T>/T
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MergeFuture<K, V> implements Future<Map<K, V>>
{
	/**
	 * Values that we have obtained.  Starts with just the preloaded values,
	 * then when the pending is complete the values get appended to this collection.
	 */
	Map<K, V> loaded;
	
	/** Pending requests - if null, this Future is complete and all values are in loaded */
	Future<Map<K, V>> pending;
	
	/**
	 * @param preloaded is a collection of entities that have already been obtained, say
	 * from the memcache.  TAKES OWNERSHIP OF THE MAP OBJECT - it will be modified later.
	 * @param pending is a future of entities that will be obtained sometime later, or
	 * null if merging is unnecessary and the preloaded values complete the result.
	 */
	public MergeFuture(Map<K, V> preloaded, Future<Map<K, V>> pending)
	{
		assert preloaded != null;
		this.loaded = preloaded;
		this.pending = pending;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#cancel(boolean)
	 */
	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		//return this.raw.cancel(mayInterruptIfRunning);
		throw new UnsupportedOperationException("This makes my head spin. Don't do it.");
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#isCancelled()
	 */
	@Override
	public boolean isCancelled()
	{
		return false;
	}

	/**
	 * 
	 * @see java.util.concurrent.Future#isDone()
	 */
	@Override
	public boolean isDone()
	{
		if (this.pending == null)
			return true;
		else
			return this.pending.isDone();
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get()
	 */
	@Override
	public Map<K, V> get() throws InterruptedException, ExecutionException
	{
		if (this.pending != null)
		{
			this.loaded.putAll(this.pending.get());
			this.pending = null;
		}
		
		return this.loaded;
	}

	/* (non-Javadoc)
	 * @see java.util.concurrent.Future#get(long, java.util.concurrent.TimeUnit)
	 */
	@Override
	public Map<K, V> get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException
	{
		if (this.pending != null)
		{
			this.loaded.putAll(this.pending.get(timeout, unit));
			this.pending = null;
		}
		
		return this.loaded;
	}
}