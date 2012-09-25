/*
 */

package com.googlecode.objectify.util;

/**
 * <p>Just holds a value.  Convenient for passing mutable values into anonymous nested classes.</p>
 * 
 * @author Jeff Schnitzer
 */
public class Holder<T>
{
	T value;
	
	public Holder(T value) {
		this.value = value;
	}
	
	public T getValue() { return value; }
	public void setValue(T value) { this.value = value; }
}