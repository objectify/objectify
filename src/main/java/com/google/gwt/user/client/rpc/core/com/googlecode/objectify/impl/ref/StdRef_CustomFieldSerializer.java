package com.google.gwt.user.client.rpc.core.com.googlecode.objectify.impl.ref;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.ref.StdRef;

public class StdRef_CustomFieldSerializer extends CustomFieldSerializer<StdRef> {

  @Override
  public void deserializeInstance(SerializationStreamReader streamReader, StdRef instance)
      throws SerializationException {
    Key<?> key = (Key<?>) streamReader.readObject();
    Object value = streamReader.readObject();
    instance = new StdRef(key, value);
  }

  @Override
  public void serializeInstance(SerializationStreamWriter streamWriter, StdRef instance) throws SerializationException {
    streamWriter.writeObject(instance.getKey());
    streamWriter.writeObject(instance.getValue());
  }

  public static void deserialize(SerializationStreamReader streamReader, StdRef instance) throws SerializationException {
    // already handled in instantiate
  }

  public static StdRef instantiate(SerializationStreamReader streamReader) throws SerializationException {
    Key<?> key = (Key<?>) streamReader.readObject();
    Object value = streamReader.readObject();
    return new StdRef(key, value);
  }

  public static void serialize(SerializationStreamWriter streamWriter, StdRef instance) throws SerializationException {
    streamWriter.writeObject(instance.getKey());
    streamWriter.writeObject(instance.getValue());
  }

}
