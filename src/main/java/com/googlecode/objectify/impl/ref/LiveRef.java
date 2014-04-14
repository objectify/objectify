package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;

import java.io.ObjectStreamException;


/**
 * <p>Implementation of Refs which are "live" and connected to the datastore so they can fetch
 * entity values even if they have not already been loaded. This is the standard Ref implementation.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class LiveRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;

	/** So that Refs can be associated with a session */
	protected transient Objectify ofy;

	/** For GWT serialization */
	protected LiveRef() {}

	/**
	 * Create a Ref based on the key
	 */
	public LiveRef(Key<T> key) {
		this(key, ObjectifyService.ofy());
	}

	/**
	 * Create a Ref based on the key, with the specified session
	 */
	public LiveRef(Key<T> key, Objectify ofy) {
		super(key);
		this.ofy = ofy;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#get()
	 */
	@Override
	public T get() {
		return ofy().load().now(key());
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.Ref#isLoaded()
	 */
	@Override
	public boolean isLoaded() {
		return ofy().isLoaded(key());
	}

	/**
	 * Get the current objectify instance associated with this ref
	 */
	private Objectify ofy() {
		// If we have an expired transaction context, we need a new context
		if (ofy == null || (ofy.getTransaction() != null && !ofy.getTransaction().isActive()))
			ofy = ObjectifyService.ofy();

		return ofy;
	}

	/**
	 * When this serializes, write out the DeadRef version. Use the getValue() for value so that
	 * if the value is not loaded, it serializes as null.
	 */
	protected Object writeReplace() throws ObjectStreamException {
		return new DeadRef<>(key(), getValue());
	}

}