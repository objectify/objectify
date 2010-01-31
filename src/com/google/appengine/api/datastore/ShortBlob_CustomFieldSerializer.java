package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore ShortBlob class
 */
public class ShortBlob_CustomFieldSerializer
{
	public static void deserialize(SerializationStreamReader streamReader, ShortBlob instance)
			throws SerializationException
	{
		// already handled in instantiate
	}

	public static ShortBlob instantiate(SerializationStreamReader streamReader)
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
		return new ShortBlob(bytes);
	}

	public static void serialize(SerializationStreamWriter streamWriter, ShortBlob instance)
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
