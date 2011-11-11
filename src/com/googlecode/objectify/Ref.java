package com.googlecode.objectify;

import java.io.Serializable;


/**
 * <p>Ref associates a Key<?> with an entity value.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Ref<T> implements Serializable, Comparable<Ref<T>>
{
	private static final long serialVersionUID = 1L;
	
	/** Key.create(Blah.class, id) is easier to type than new Key<Blah>(Blah.class, id) */
	public static <T> Ref<T> create(Key<T> key) {
		return new Ref<T>(key);
	}

	/** If this Ref is loaded with a */
	protected Key<T> key;

	/** If this Ref is loaded with a */
	protected Result<T> result;
	
	/** For GWT serialization */
	protected Ref() {}

	/** Create a Ref based on the key */
	public Ref(Key<T> key) {
		this.key = key;
	}
	
	/**
	 * @return the key associated with this Ref
	 */
	public Key<T> key() {
		return key;
	}
	
	/**
	 * Same as key() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	public Key<T> getKey() {
		return key();
	}
	
	/**
	 * Obtain the entity value if it has been initialized.
	 * @return the entity referenced
	 * @throws IllegalStateException if the value has not been initialized (say, through a database fetch)
	 */
	public T value() {
		if (this.result == null)
			throw new IllegalStateException("Ref<?> value has not been initialized");
		else
			return this.result.get();
	}

	/**
	 * Same as value() but conforms to JavaBeans conventions in case this is being processed by a JSON
	 * converter or expression language.
	 */
	public T getValue() {
		return value();
	}

	/** Comparison is based on key */
	@Override
	public int compareTo(Ref<T> o) {
		return this.key.compareTo(o.key);
	}
}