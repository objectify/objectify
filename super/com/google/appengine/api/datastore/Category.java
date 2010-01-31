package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class Category implements Serializable, Comparable<Category>
{
	private final String category;

	public Category(String category)
	{
		if (category == null)
		{
			throw new NullPointerException("category must not be null");
		}
		else
		{
			this.category = category;
		}
	}

	@SuppressWarnings("unused")
	private Category()
	{
		category = null;
	}

	public String getCategory()
	{
		return category;
	}

	public int compareTo(Category o)
	{
		return category.compareTo(o.category);
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Category other = (Category) o;
		return category.equals(other.category);
	}

	public int hashCode()
	{
		return category.hashCode();
	}
}
