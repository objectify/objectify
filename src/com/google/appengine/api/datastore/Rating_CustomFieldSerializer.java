package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Rating class
 */
public class Rating_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Rating instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Rating instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new Rating(streamReader.readInt());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Rating instance)
			throws SerializationException
	{
		streamWriter.writeInt(instance.getRating());
	}
}
