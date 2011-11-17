/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

/**
 * A fruit.
 * 
 * @author Scott Hernandez
 */
@Entity
@Cache
public abstract class Fruit
{
	@Id Long id;
	String color;
	String taste;
	
	/** Default constructor must always exist */
	protected Fruit() {}
	
	/** Constructor*/
	protected Fruit(String color, String taste)
	{
		this.color = color;
		this.taste = taste;
	}
	
	public String getColor()
	{
		return this.color;
	}
	
	public String getTaste()
	{
		return this.taste;
	}
}