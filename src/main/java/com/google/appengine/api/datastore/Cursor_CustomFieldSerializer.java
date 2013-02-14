package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Cursor class
 */
public class Cursor_CustomFieldSerializer extends CustomFieldSerializer<Cursor>
{
	public static void deserialize(SerializationStreamReader streamReader, Cursor instance) {
	}

	public static Cursor instantiate(SerializationStreamReader streamReader) throws SerializationException {
		return Cursor.fromWebSafeString(streamReader.readString());
	}

	public static void serialize(SerializationStreamWriter streamWriter, Cursor instance) throws SerializationException {
		streamWriter.writeString(instance.toWebSafeString());
	}

	@Override
	public Cursor instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader, Cursor instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter, Cursor instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
