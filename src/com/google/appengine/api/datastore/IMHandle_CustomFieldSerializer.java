package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore IMHandle class
 */
public class IMHandle_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, IMHandle instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static IMHandle instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		String s = streamReader.readString();
		String address = streamReader.readString();
		IMHandle.Scheme scheme = IMHandle.Scheme.valueOf(s);
		return new IMHandle(scheme, address);
	}

	public static void serialize(SerializationStreamWriter streamWriter, IMHandle instance)
			throws SerializationException
	{
		streamWriter.writeString(instance.getProtocol());
		streamWriter.writeString(instance.getAddress());
	}
}
