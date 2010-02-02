package com.googlecode.objectify.test.entity;

import com.google.appengine.api.datastore.Category;
import com.google.appengine.api.datastore.Rating;

/**
 */
public class Industry
{
	public Category category;
	public Rating strength;

	public Industry() { }

	public Industry(Category category, Rating strength)
	{
		this.category = category;
		this.strength = strength;
	}
}
