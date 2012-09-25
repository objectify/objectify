package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Link class
 */
public class Link_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Link instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Link instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new Link(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Link instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getValue());
	}
}
