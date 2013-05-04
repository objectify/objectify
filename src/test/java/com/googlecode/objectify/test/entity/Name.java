package com.googlecode.objectify.test.entity;

import java.io.Serializable;

import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.util.LangUtils;

/**
 */
@Embed
@SuppressWarnings("serial")
public class Name implements Serializable
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

	public boolean equals(Object o)
	{
		return o != null
			&& o.getClass() == this.getClass()
			&& LangUtils.objectsEqual(((Name)o).firstName, this.firstName)
			&& LangUtils.objectsEqual(((Name)o).lastName, this.lastName);
	}

	@Override
	public int hashCode()
	{
		if (firstName == null)
			return 0;
		else
			return firstName.hashCode();
	}
}
