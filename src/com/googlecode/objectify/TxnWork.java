package com.googlecode.objectify;


/**
 * For executing transactions, this is a unit of work.  Typically you will extend Objectify.Work
 * and not this class, which lets you create other kinds of Work using ObjectifyWrapper.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface TxnWork<O extends Objectify, R>
{
	R run(O ofy);
}
