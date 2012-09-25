package com.googlecode.objectify.util;

import java.util.Iterator;

/**
 * Iterator wrapper that translates from one type to another
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class TranslatingIterator<F, T> implements Iterator<T>
{
	/** */
	protected Iterator<F> base;
	
	/** */
	public TranslatingIterator(Iterator<F> base) 
	{
		this.base = base;
	}
	
	/**
	 * You implement this - convert from one object to the other
	 */
	abstract protected T translate(F from); 

	@Override
	public boolean hasNext()
	{
		return this.base.hasNext();
	}

	@Override
	public T next()
	{
		return this.translate(this.base.next());
	}

	@Override
	public void remove()
	{
		this.base.remove();
	}
}
