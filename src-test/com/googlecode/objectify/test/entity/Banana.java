/*
 */

package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;


/**
 * A banana fruit.
 * 
 * @author Scott Hernandez
 */
@Entity
@Cache
public class Banana extends Fruit
{
	public static final String COLOR = "yellow";
	public static final String TASTE = "sweet";
	
	private String shape;
	
	/** Default constructor must always exist */
	public Banana() {}
	
	/** Constructor*/
	public Banana(String color, String taste)
	{
		super(color,taste);
		this.shape = "like a banana";
	}
	
	public String getShape() 
	{
		return this.shape;
	}
}