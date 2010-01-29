package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
public class Link implements Serializable, Comparable<Link>
{
	private final String value;

	private Link()
	{
		value = null;
	}

	public Link(String value)
	{
		this.value = value;
	}

	public String getValue()
	{
		return value;
	}

	public int hashCode()
	{
		return value.hashCode();
	}

	public boolean equals(Object object)
	{
		if (object instanceof Link)
		{
			Link key = (Link) object;
			return value.equals(key.value);
		}
		else
		{
			return false;
		}
	}

	public String toString()
	{
		return value;
	}

	public int compareTo(Link l)
	{
		return value.compareTo(l.value);
	}
}
