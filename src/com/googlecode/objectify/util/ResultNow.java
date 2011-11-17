package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

/**
 * Simplistic result that holds a constant value.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ResultNow<T> implements Result<T>
{
	private T value;
	
	public ResultNow(T value) {
		this.value = value;
	}

	@Override
	public T now() {
		return value;
	}
}