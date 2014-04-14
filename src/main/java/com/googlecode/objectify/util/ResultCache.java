package com.googlecode.objectify.util;

import com.googlecode.objectify.Result;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Caches a result value so it is only executed once
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class ResultCache<T> implements Result<T>, Serializable
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

			this.postExecuteHook();
		}

		return value;
	}

	/** Executed once after the cached value is assigned. */
	protected void postExecuteHook() {}

	/**
	 * When this serializes, write out a simple version that doesn't hold complicated links to internal
	 * structures.  This is safe as long as nobody ever tries to cast to ResultCache directly, which should
	 * never happen.
	 */
	protected Object writeReplace() throws ObjectStreamException {
		return new ResultNow<>(now());
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