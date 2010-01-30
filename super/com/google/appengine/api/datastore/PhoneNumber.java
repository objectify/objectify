package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class PhoneNumber implements Serializable, Comparable<PhoneNumber>
{
	private final String number;

	public PhoneNumber(String number)
	{
		if (number == null)
		{
			throw new NullPointerException("number must not be null");
		}
		else
		{
			this.number = number;
			return;
		}
	}

	@SuppressWarnings("unused")
	private PhoneNumber()
	{
		number = null;
	}

	public String getNumber()
	{
		return number;
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PhoneNumber that = (PhoneNumber) o;
		return number.equals(that.number);
	}

	public int hashCode()
	{
		return number.hashCode();
	}

	public int compareTo(PhoneNumber o)
	{
		return number.compareTo(o.number);
	}
}
