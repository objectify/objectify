package com.googlecode.objectify.test.entity;

import com.googlecode.objectify.annotation.Cached;

/**
 */
@Cached
public class Name
{
	public String firstName;
	public String lastName;

	public Name()
	{
	}

	public Name(String firstName, String lastName)
	{
		this.firstName = firstName;
		this.lastName = lastName;
	}
}
