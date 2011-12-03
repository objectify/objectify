package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

/**
 * Wraps a Result, translating from one type to another and caching the result
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultWrapper<F, T> extends ResultTranslator<Result<F>, T>
{
	public ResultWrapper(Result<F> base) {
		super(base);
	}

	protected abstract T wrap(F orig);
	
	@Override
	final protected T translate(Result<F> from) {
		return wrap(from.now());
	}

	/** We can get away with this because the subclass almost always provides concrete types */
	@Override
	public String toString() {
		//Type from = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[0]);
		//Type to = GenericTypeReflector.getTypeParameter(this.getClass(), ResultTranslator.class.getTypeParameters()[1]);
		
		return GenericTypeReflector.getExactSuperType(this.getClass(), ResultWrapper.class).toString();
	}
}