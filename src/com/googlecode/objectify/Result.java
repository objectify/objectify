package com.googlecode.objectify;



/**
 * <p>
 * This interface provides a return value for asynchronous Objectify calls,
 * nearly the same as {@code java.util.concurrent.Future}. Unfortunately the methods
 * of {@code Future} throw checked exceptions, rendering the class prohibitively
 * painful to use in business logic.  This interface fixes that problem, and implementors
 * automatically unwrap ExecutionExceptions (checked exceptions will be wrapped in a
 * new RuntimeException).
 * </p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public interface Result<T>
{
	/**
	 * Waits if necessary for the computation to complete, and then retrieves
	 * its result.  If the computation produced an exception, it will be thrown here.
	 * 
	 * @return the computed result
	 */
	T now();
}