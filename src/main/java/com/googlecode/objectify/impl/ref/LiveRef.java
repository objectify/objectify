package com.googlecode.objectify.impl.ref;

import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;


/**
 * <p>Implementation of Refs which are "live" and connected to the datastore so they can fetch
 * entity values even if they have not already been loaded.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
abstract public class LiveRef<T> extends Ref<T>
{
	private static final long serialVersionUID = 1L;

	/** So that Refs can be associated with a session */
	protected transient Objectify ofy;

	/** For GWT serialization */
	protected LiveRef() {}

	/**
	 * Live refs are associated with an objectify session
	 */
	public LiveRef(Objectify ofy) {
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
		if (ofy == null || (ofy.getTxn() != null && !ofy.getTxn().isActive()))
			ofy = ObjectifyService.ofy();

		return ofy;
	}
}