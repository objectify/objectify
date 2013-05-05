package com.googlecode.objectify.impl.ref;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.googlecode.objectify.Key;

/**
 * Custom field serializer for the DeadRef class.
 */
public class DeadRef_CustomFieldSerializer extends CustomFieldSerializer<DeadRef<?>> {

	public static void deserialize(SerializationStreamReader streamReader, DeadRef<?> instance) {
	}

	public static DeadRef<?> instantiate(SerializationStreamReader streamReader) throws SerializationException {
		@SuppressWarnings("unchecked")
		Key<Object> key = (Key<Object>)streamReader.readObject();
		Object value = streamReader.readObject();

		return new DeadRef<Object>(key, value);
	}

	public static void serialize(SerializationStreamWriter streamWriter, DeadRef<?> instance) throws SerializationException {
		streamWriter.writeObject(instance.key());
		streamWriter.writeObject(instance.getValue());
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	public DeadRef<?> instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}

	@Override
	public void deserializeInstance(SerializationStreamReader streamReader, DeadRef<?> instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter, DeadRef<?> instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
