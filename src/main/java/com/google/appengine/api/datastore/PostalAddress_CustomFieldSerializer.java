package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore PostalAddress class
 */
public class PostalAddress_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, PostalAddress instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static PostalAddress instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new PostalAddress(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, PostalAddress instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getAddress());
	}
}
