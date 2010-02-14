package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Cursor class
 */
public class Cursor_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Cursor instance)
			throws SerializationException
	{
		// already handled in instantiate
	}


	public static Cursor instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return Cursor.fromWebSafeString(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Cursor instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.toWebSafeString());
	}
}
