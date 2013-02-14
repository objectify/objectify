package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore Key class. Does not transmit appid,
 * just parent/kind/id/name.
 */
public class Key_CustomFieldSerializer extends CustomFieldSerializer<Key> {

	public static void deserialize(SerializationStreamReader streamReader, Key instance) {
	}

	public static Key instantiate(SerializationStreamReader streamReader) throws SerializationException {
		Key parent = (Key) streamReader.readObject();
		String kind = streamReader.readString();
		long id = streamReader.readLong();
		String name = streamReader.readString();

		// @SuppressWarnings("unused")
		// AppIdNamespace appIdNamespace =
		// (AppIdNamespace)streamReader.readObject();

		if (name == null)
			return KeyFactory.createKey(parent, kind, id);
		else
			return KeyFactory.createKey(parent, kind, name);
	}

	public static void serialize(SerializationStreamWriter streamWriter, Key instance) throws SerializationException {
		streamWriter.writeObject(instance.getParent());
		streamWriter.writeString(instance.getKind());
		streamWriter.writeLong(instance.getId());
		streamWriter.writeString(instance.getName());
		// streamWriter.writeObject(instance.getAppIdNamespace());
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	public Key instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader, Key instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter, Key instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
