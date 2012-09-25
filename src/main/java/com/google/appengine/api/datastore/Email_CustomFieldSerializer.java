package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Email class
 */
public class Email_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Email instance)
			throws SerializationException
	{
		// already handled in instantiate
	}


	public static Email instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new Email(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Email instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getEmail());
	}
}
