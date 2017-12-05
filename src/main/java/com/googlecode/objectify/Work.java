package com.googlecode.objectify;


/**
 * For executing transactions, this is a unit of work.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Work<R> {
	R run();
}
