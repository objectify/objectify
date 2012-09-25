package com.googlecode.objectify;


/**
 * Just like the EJB options.  See http://docs.oracle.com/javaee/6/api/javax/ejb/TransactionAttributeType.html
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public enum TxnType
{
	/** Require that there must already be a transaction running.  If no transaction, throw an IllegalStateException. */
	MANDATORY,
	
	/** Require that there must NOT be a transaction running.  If there is, throw an IllegalStateException. */
	NEVER,

	/** Execute the work without a transaction, pausing an existing transaction if there is one. */
	NOT_SUPPORTED,
	
	/** Use the existing transaction (if present), or start a new transaction if not. */
	REQUIRED,
	
	/** Start a new transaction, pausing the old one. */
	REQUIRES_NEW,
	
	/** Inherits previous transaction state. */
	SUPPORTS;
}