package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Key class.
 * Does not transmit appid, just parent/kind/id/name.
 */
public class Key_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Key instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Key instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		Key parent = (Key) streamReader.readObject();
		String kind = streamReader.readString();
		long id = streamReader.readLong();
		String name = streamReader.readString();

		if (name == null)
			return KeyFactory.createKey(parent, kind, id);
		else
			return KeyFactory.createKey(parent, kind, name);
	}

	public static void serialize(SerializationStreamWriter streamWriter, Key instance)
			throws SerializationException
	{
		streamWriter.writeObject(instance.getParent());
		streamWriter.writeString(instance.getKind());
		streamWriter.writeLong(instance.getId());
		streamWriter.writeString(instance.getName());
	}
}
