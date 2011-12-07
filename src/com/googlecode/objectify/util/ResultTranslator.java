package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

/**
 * Translates from one arbitrary thing to a Result of another arbitrary thing, caching the value.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultTranslator<F, T> implements Result<T>
{
	private F from;
	private boolean translated;
	private T to;
	
	public ResultTranslator(F from) {
		this.from = from;
	}

	protected abstract T translate(F from);
	
	@Override
	public T now() {
		if (!translated) {
			to = translate(from);
			translated = true;
		}
		
		return to;
	}
	
	/** We can get away with this because the subclass almost always provides concrete types */
	@Override
	public String toString() {
		//Type from = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[0]);
		//Type to = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[1]);
		
		return GenericTypeReflector.getExactSuperType(this.getClass(), ResultTranslator.class).toString();
	}
}