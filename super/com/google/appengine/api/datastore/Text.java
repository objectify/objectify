package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class Text implements Serializable
{
	private final String value;

	public Text(String value)
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
		if (object instanceof Text)
		{
			Text key = (Text) object;
			return value.equals(key.value);
		}
		else
		{
			return false;
		}
	}

	public String toString()
	{
		String text = value;
		if (text.length() > 70)
			text = text.substring(0, 70) + "...";

		return "<Text: " + text + ">";
	}
}
