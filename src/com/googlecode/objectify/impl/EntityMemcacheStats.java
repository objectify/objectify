package com.googlecode.objectify.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.cache.MemcacheStats;

/** 
 * Tracks hit/miss statistics for the memcache. 
 */
public class EntityMemcacheStats implements MemcacheStats
{
	/** */
	public class Stat
	{
		private AtomicInteger hits = new AtomicInteger();
		private AtomicInteger misses = new AtomicInteger();
		
		public int getHits() { return this.hits.get(); }
		public int getMisses() { return this.misses.get(); }

		public float getPercent()
		{
			int h = this.getHits();
			int m = this.getMisses();
			
			if (m == 0)
				return 0;
			else
				return (float)h / (float)m;
		}
	}
	
	/** */
	private ConcurrentHashMap<String, Stat> stats = new ConcurrentHashMap<String, Stat>();
	
	/**
	 * Get the live statistics.  You can clear it if you want. 
	 *  
	 * @return the live map, but you can iterate through it just fine 
	 */
	public ConcurrentHashMap<String, Stat> getStats() { return this.stats; }

	/** */
	@Override
	public void recordHit(Key key)
	{
		this.getStat(key.getKind()).hits.incrementAndGet();
	}

	/** */
	@Override
	public void recordMiss(Key key)
	{
		this.getStat(key.getKind()).misses.incrementAndGet();
	}

	/**
	 * We're just tracking statistics so we don't really need to worry about these stepping on each other;
	 * if there's a hit or miss lost no big deal.
	 */
	private Stat getStat(String kind)
	{
		Stat stat = this.stats.get(kind);
		if (stat == null)
		{
			stat = new Stat();
			this.stats.put(kind, stat);
		}
		
		return stat;
	}
}