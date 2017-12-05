package com.googlecode.objectify;


/**
 * Using Work<Void> is annoying because you must return a value from the run() method.  Using
 * VoidWork eliminates that annoyance.  Unfortunately we can't override the return value of
 * a method so we must rename run() to vrun().
 *
 * @deprecated There are overloads for {@transact()} etc that take a {@Runnable} instead.
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Deprecated
abstract public class VoidWork implements Work<Void>
{
	public final Void run() {
		vrun();
		return null;
	}
	
	public abstract void vrun();
}
