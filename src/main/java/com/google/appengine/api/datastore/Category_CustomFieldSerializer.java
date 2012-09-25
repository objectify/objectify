package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Category class
 */
public class Category_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Category instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Category instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new Category(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Category instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getCategory());
	}
}
