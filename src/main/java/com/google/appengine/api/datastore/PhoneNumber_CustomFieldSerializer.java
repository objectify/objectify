package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore PhoneNumber class
 */
public class PhoneNumber_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, PhoneNumber instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static PhoneNumber instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new PhoneNumber(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, PhoneNumber instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getNumber());
	}
}
