package com.google.appengine.api.datastore;

import java.io.Serializable;

/**
 * GWT emulation class.
 */
@SuppressWarnings("serial")
public class IMHandle implements Serializable, Comparable<IMHandle>
{
	public static enum Scheme
	{
		sip, unknown, xmpp
	}

	private final String protocol;
	private final String address;

	public IMHandle(Scheme scheme, String address)
	{
		if (scheme == null)
		{
			throw new NullPointerException("scheme must not be null");
		}
		else
		{
			validateAddress(address);
			protocol = scheme.name();
			this.address = address;
		}
	}

	@SuppressWarnings("unused")
	private IMHandle()
	{
		protocol = null;
		address = null;
	}

	private static void validateAddress(String address)
	{
		if (address == null)
			throw new NullPointerException("address must not be null");
	}

	public String getProtocol()
	{
		return protocol;
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
		IMHandle imHandle = (IMHandle) o;
		if (!address.equals(imHandle.address))
			return false;
		return protocol.equals(imHandle.protocol);
	}

	public int hashCode()
	{
		int result = protocol.hashCode();
		result = 31 * result + address.hashCode();
		return result;
	}

	public int compareTo(IMHandle o)
	{
		return toString().compareTo(o.toString());
	}

	public String toString()
	{
		return protocol + " " + address;
	}
}
