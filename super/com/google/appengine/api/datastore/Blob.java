package com.google.appengine.api.datastore;

import java.io.Serializable;
import java.util.Arrays;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class Blob implements Serializable
{
	private final byte bytes[];

	public Blob(byte bytes[])
	{
		this.bytes = bytes;
	}

	public byte[] getBytes()
	{
		return bytes;
	}

	public int hashCode()
	{
		return Arrays.hashCode(bytes);
	}

	public boolean equals(Object object)
	{
		if(object instanceof Blob)
		{
			Blob key = (Blob)object;
			return Arrays.equals(bytes, key.bytes);
		} else
		{
			return false;
		}
	}

	public String toString()
	{
		return "<Blob: " + bytes.length + " bytes>";
	}

}
