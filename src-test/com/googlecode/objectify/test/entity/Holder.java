/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * A holder of a <T>hing.
 * 
 * @author Scott Hernandez
 */
@Entity
@Cache
public abstract class Holder<T>
{	
	@Id Long id;
	T thing;
	
	/** Default constructor must always exist */
	protected Holder() {}
	protected Holder(T t) {this.thing = t;}
	
	public T getThing()
	{
		return this.thing;
	}
	public void setThing(T t)
	{
		this.thing = t;
	}
}