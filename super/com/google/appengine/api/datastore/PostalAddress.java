package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class PostalAddress implements Serializable, Comparable<PostalAddress>
{
	private final String address;

	public PostalAddress(String address)
	{
		if (address == null)
		{
			throw new NullPointerException("address must not be null");
		}
		else
		{
			this.address = address;
		}
	}

	@SuppressWarnings("unused")
	private PostalAddress()
	{
		address = null;
	}

	public String getAddress()
	{
		return address;
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		PostalAddress that = (PostalAddress) o;
		return address.equals(that.address);
	}

	public int hashCode()
	{
		return address.hashCode();
	}

	public int compareTo(PostalAddress o)
	{
		return address.compareTo(o.address);
	}
}
