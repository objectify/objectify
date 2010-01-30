package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class Email implements Serializable, Comparable<Email>
{
	private final String email;

	@SuppressWarnings("unused")
	private Email()
	{
		this.email = null;
	}

	public Email(String email)
	{
		if (email == null)
		{
			throw new NullPointerException("email must not be null");
		}
		else
		{
			this.email = email;
		}
	}

	public String getEmail()
	{
		return email;
	}

	public int compareTo(Email e)
	{
		return email.compareTo(e.email);
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Email other = (Email) o;
		return email.equals(other.email);
	}

	public int hashCode()
	{
		return email.hashCode();
	}

	public String toString()
	{
		return email;
	}
}
