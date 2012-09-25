package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Blob class
 */
public class Blob_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, Blob instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static Blob instantiate(SerializationStreamReader streamReader)
			throws SerializationException
	{
		byte[] bytes;
		int len = streamReader.readInt();
		if (len == -1) {
			bytes = null;
		} else {
			bytes = new byte[len];
			for (int i = 0; i < len; i++) {
				bytes[i] = streamReader.readByte();
			}
		}
		return new Blob(bytes);
	}

	public static void serialize(SerializationStreamWriter streamWriter, Blob instance)
			throws SerializationException
	{
		byte[] bytes = instance.getBytes();
		if (bytes == null) {
			streamWriter.writeInt(-1);
		} else {
			streamWriter.writeInt(bytes.length);
			for (byte b : bytes)
			{
				streamWriter.writeByte(b);
			}
		}
	}
}
