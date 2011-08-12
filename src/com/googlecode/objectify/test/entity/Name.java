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
			&& safeEquals(((Name)o).firstName, this.firstName)
			&& safeEquals(((Name)o).lastName, this.lastName);
	}
	
	@Override
	public int hashCode()
	{
		return firstName.hashCode();
	}

	/**
	 * Null safe equality comparison
	 */
	boolean safeEquals(Object o1, Object o2)
	{
		return (o1 == o2) || ((o1 != null) && o1.equals(o2));
	}
}
