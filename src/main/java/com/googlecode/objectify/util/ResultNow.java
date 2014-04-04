package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

import java.io.Serializable;

/**
 * Simplistic result that holds a constant value.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ResultNow<T> implements Result<T>, Serializable
{
	private static final long serialVersionUID = 1L;

	private T value;

	public ResultNow(T value) {
		this.value = value;
	}

	@Override
	public T now() {
		return value;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + value + ")";
	}
}