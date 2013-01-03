package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class Rating implements Serializable, Comparable<Rating>
{
	public static final int MIN_VALUE = 0;
	public static final int MAX_VALUE = 100;
	private final int rating;

	public Rating(int rating)
	{
		if (rating < MIN_VALUE || rating > MAX_VALUE)
		{
			throw new IllegalArgumentException("Rating must be between " + MIN_VALUE + " and " + MAX_VALUE);
		}
		else
		{
			this.rating = rating;
		}
	}

	@SuppressWarnings("unused")
	private Rating()
	{
		this(0);
	}

	public int getRating()
	{
		return rating;
	}

	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Rating rating1 = (Rating) o;
		return rating == rating1.rating;
	}

	public int hashCode()
	{
		return rating;
	}

	public int compareTo(Rating o)
	{
		return Integer.valueOf(rating).compareTo(Integer.valueOf(o.rating));
	}
}
