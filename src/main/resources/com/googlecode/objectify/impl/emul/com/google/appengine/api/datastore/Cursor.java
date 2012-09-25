package com.google.appengine.api.datastore;

import java.io.Serializable;
import java.lang.String;

/**
 * GWT emulation class. Will not have the same hashCode() value as the non-emulated version.
 * This class will not have the same toString() value as the non-emulated version.
 */
@SuppressWarnings("serial")
public final class Cursor implements Serializable
{
	private final String webString;

	public Cursor(String webString)
	{
		this.webString = webString;
	}

	public String toWebSafeString()
	{
		return webString;
	}

	public static Cursor fromWebSafeString(String encodedCursor)
	{
		if (encodedCursor == null)
			throw new NullPointerException("encodedCursor must not be null");

		return new Cursor(encodedCursor);
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		Cursor cursor = (Cursor) o;

		if (!webString.equals(cursor.webString)) return false;

		return true;
	}

	@Override
	public int hashCode()
	{
		return webString.hashCode();
	}

	public String toString()
	{
		return webString;
	}

}
