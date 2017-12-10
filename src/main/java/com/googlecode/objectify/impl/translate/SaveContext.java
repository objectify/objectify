package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.impl.LoadConditions;

/**
 * The context of a save operation; might involve multiple entities (eg, batch save).
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SaveContext
{
	/**
	 * Subclass can ignore lifecycle methods.
	 */
	public boolean skipLifecycle() {
		return false;
	}

	/**
	 * Callback that we found a Ref in the object graph. Subclasses of this context may want to do something
	 * special with this.
	 */
	public Key saveRef(final Ref<?> value, final LoadConditions loadConditions) {
		return value.key().getRaw();
	}
}