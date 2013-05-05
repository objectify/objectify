package com.googlecode.objectify.impl.ref;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.googlecode.objectify.Key;

/**
 * Custom field serializer for the LiveRef class.
 */
public class LiveRef_CustomFieldSerializer extends CustomFieldSerializer<LiveRef<?>> {

	public static void deserialize(SerializationStreamReader streamReader, LiveRef<?> instance) {
	}

	public static LiveRef<?> instantiate(SerializationStreamReader streamReader) throws SerializationException {
		@SuppressWarnings("unchecked")
		Key<Object> key = (Key<Object>)streamReader.readObject();
		Object value = streamReader.readObject();

		// The difference from server code is that we keep the value on the client
		return new LiveRef<Object>(key, value);
	}

	public static void serialize(SerializationStreamWriter streamWriter, LiveRef<?> instance) throws SerializationException {
		streamWriter.writeObject(instance.key());
		streamWriter.writeObject(instance.getValue());
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	public LiveRef<?> instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader, LiveRef<?> instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter, LiveRef<?> instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
