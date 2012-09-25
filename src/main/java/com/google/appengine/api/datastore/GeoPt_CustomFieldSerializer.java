package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore GeoPt class
 */
public class GeoPt_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, GeoPt instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static GeoPt instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		return new GeoPt(streamReader.readFloat(), streamReader.readFloat());
	}

	public static void serialize(SerializationStreamWriter streamWriter, GeoPt instance)
			throws SerializationException
	{
		streamWriter.writeFloat(instance.getLatitude());
		streamWriter.writeFloat(instance.getLongitude());
	}
}
