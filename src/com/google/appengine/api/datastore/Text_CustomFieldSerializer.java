package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Text class
 */
public class Text_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Text instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Text instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new Text(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Text instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getValue());
	}
}
