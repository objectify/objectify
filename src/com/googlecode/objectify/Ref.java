package com.googlecode.objectify;

import java.io.Serializable;

import com.googlecode.objectify.impl.StdRef;


/**
 * <p>Ref associates a Key<?> with an entity value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class Ref<T> implements Serializable, Comparable<Ref<T>>
{
	private static final long serialVersionUID = 1L;
	
	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Ref<T> create(Key<T> key) {
		return new StdRef<T>(key);
	}

	/** If this Ref is loaded with a */
	protected Result<T> result;
	
	/**
	 * @return the key associated with this Ref
	 */
	abstract public Key<T> key();
	
	/**
	 * Same as key() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	public Key<T> getKey() {
		return key();
	}
	
	/**
	 * Obtain the entity value if it has been initialized.
	 * 
	 * @return the entity referenced, or null if the entity was not found
	 * @throws IllegalStateException if the value has not been initialized (say, through a database fetch)
	 */
	@SuppressWarnings("unchecked")
	public <E extends T> E get() {
		if (this.result == null)
			throw new IllegalStateException("Ref<?> value has not been initialized");
		else
			return (E)this.result.now();
	}
	
	/**
	 * Obtain the entity value if it has been initialized, throwing an exception if the entity was not found.
	 * 
	 * @return the entity referenced
	 * @throws NotFoundException if the specified entity was not found
	 * @throws IllegalStateException if the value has not been initialized (say, through a database fetch)
	 */
	public <E extends T> E getSafe() {
		E t = this.get();
		if (t == null)
			throw new NotFoundException(key());
		else
			return t;
	}

	/**
	 * Same as get() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	public <E extends T> E getValue() {
		return get();
	}
	
	/**
	 * Assigns a new result object, replacing (or initializing) the present one.
	 */
	public void setResult(Result<T> result) {
		this.result = result;
	}
	
	/** Comparison is based on key */
	@Override
	public int compareTo(Ref<T> o) {
		return this.key().compareTo(o.key());
	}
}