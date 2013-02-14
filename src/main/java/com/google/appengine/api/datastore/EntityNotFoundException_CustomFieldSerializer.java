package com.google.appengine.api.datastore;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

/**
 * Custom field serializer for the datastore EntityNotFoundException class
 */
public class EntityNotFoundException_CustomFieldSerializer extends CustomFieldSerializer<EntityNotFoundException>
{
	public static void deserialize(SerializationStreamReader streamReader, EntityNotFoundException instance) {
	}

	public static EntityNotFoundException instantiate(SerializationStreamReader streamReader) throws SerializationException {
		// Not sure why this isn't working
		//return new EntityNotFoundException((Key)streamReader.readObject());

		return new EntityNotFoundException(null);
	}

	public static void serialize(SerializationStreamWriter streamWriter, EntityNotFoundException instance) throws SerializationException {
		//streamWriter.writeObject(instance.getKey());
	}

	@Override
	public boolean hasCustomInstantiateInstance() {
		return true;
	}

	@Override
	public EntityNotFoundException instantiateInstance(SerializationStreamReader streamReader) throws SerializationException {
		return instantiate(streamReader);
	}
	@Override
	public void deserializeInstance(SerializationStreamReader streamReader, EntityNotFoundException instance) throws SerializationException {
		deserialize(streamReader, instance);
	}

	@Override
	public void serializeInstance(SerializationStreamWriter streamWriter, EntityNotFoundException instance) throws SerializationException {
		serialize(streamWriter, instance);
	}
}
