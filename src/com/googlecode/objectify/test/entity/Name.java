package com.googlecode.objectify.test.entity;

import java.io.Serializable;


/**
 */
public class Name implements Serializable
{
	private static final long serialVersionUID = 1L;
	
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
	
	public boolean equals(Object o)
	{
		// Doesn't do null check on names but good enough
		return o != null
			&& o.getClass() == this.getClass()
			&& ((Name)o).firstName.equals(this.firstName)
			&& ((Name)o).lastName.equals(this.lastName);
	}
}
