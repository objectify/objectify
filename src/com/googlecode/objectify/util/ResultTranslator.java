package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

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
}