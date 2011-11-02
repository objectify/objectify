package com.googlecode.objectify;

import java.io.Serializable;
import java.util.Iterator;

import com.googlecode.objectify.util.TranslatingIterator;

/**
 * <p>This is a typesafe version of the KeyRange object.  It represents a number
 * of ids preallocated with {@code ObjectifyFactory#allocateIds(Class, long)}.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyRange<T> implements Iterable<Key<T>>, Serializable
{
	private static final long serialVersionUID = 1L;
	
	/** */
	com.google.appengine.api.datastore.KeyRange raw;
	
	/** */
	KeyRange(com.google.appengine.api.datastore.KeyRange raw)
	{
		this.raw = raw;
	}
	
	/**
	 * Get the raw datastore keyrange.
	 */
	public com.google.appengine.api.datastore.KeyRange getRaw() { return this.raw; }

	/* (non-Javadoc)
	 * @see java.lang.Iterable#iterator()
	 */
	@Override
	public Iterator<Key<T>> iterator()
	{
		return new TranslatingIterator<com.google.appengine.api.datastore.Key, Key<T>>(this.raw.iterator()) {
			@Override
			protected Key<T> translate(com.google.appengine.api.datastore.Key from)
			{
				return new Key<T>(from);
			}
		};
	}
}