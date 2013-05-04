package com.googlecode.objectify;


/**
 * Exception thrown when a fetch for something could not be found.  This is associated with the
 * getSafe() and keySafe() methods on Ref; if the item being sought in the Ref couldn't be found,
 * this will be thrown.
 */
public class NotFoundException extends RuntimeException
{
	private static final long serialVersionUID = 1L;

	/** */
	private Key<?> key;

	/** Thrown when there is no key context (eg, query.first() on an empty result set) */
	public NotFoundException() {
		this(null);
	}

	/** Thrown when we at least know what we are looking for! */
	public NotFoundException(Key<?> key) {
		super(key == null ? "No entity was found" : "No entity was found matching the key: " + key);
		this.key = key;
	}

	/** @return the key we are looking for, if known */
	public Key<?> getKey() {
		return this.key;
	}
}
