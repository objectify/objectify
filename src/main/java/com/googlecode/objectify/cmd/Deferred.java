package com.googlecode.objectify.cmd;

/**
 * <p>The top element in the command chain for deferred operations.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Deferred {
	/**
	 * <p>Start a deferred save command chain.  Allows you to save (or re-save) entity objects.  Note that all command
	 * chain objects are immutable.</p>
	 *
	 * <p>Saves do NOT cascade; if you wish to save an object graph, you must save each individual entity.</p>
	 *
	 * <p>A quick example:
	 * {@code ofy().defer().save().entities(e1, e2, e3);}</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return the next step in the immutable command chain.
	 */
	DeferredSaver save();

	/**
	 * <p>Start a deferred delete command chain.  Lets you delete entities or keys.</p>
	 *
	 * <p>Deletes do NOT cascade; if you wish to delete an object graph, you must delete each individual entity.</p>
	 *
	 * <p>A quick example:
	 * {@code ofy().defer().delete().entities(e1, e2, e3);}</p>
	 *
	 * <p><b>All command objects are immutable; this method returns a new object rather than modifying the
	 * current command object.</b></p>
	 *
	 * @return the next step in the immutable command chain.
	 */
	DeferredDeleter delete();

}
