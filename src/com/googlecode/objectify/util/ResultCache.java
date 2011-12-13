package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

/**
 * Caches a result value so it is only executed once
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultCache<T> implements Result<T>
{
	private boolean cached;
	private T value;
	
	/** */
	protected abstract T nowUncached();
	
	/** */
	protected boolean isExecuted() {
		return cached;
	}
	
	/** */
	@Override
	final public T now() {
		if (!cached) {
			value = nowUncached();
			cached = true;
		}
		
		return value;
	}

	/** We can get away with this because the subclass almost always provides concrete types */
	@Override
	public String toString() {
		//Type from = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[0]);
		//Type to = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[1]);
		
		//return GenericTypeReflector.getExactSuperType(this.getClass(), ResultTranslator.class).toString();
		
		if (isExecuted())
			return "ResultCache(" + value + ")";
		else
			return "ResultCache/" + this.getClass().getName();
	}
}