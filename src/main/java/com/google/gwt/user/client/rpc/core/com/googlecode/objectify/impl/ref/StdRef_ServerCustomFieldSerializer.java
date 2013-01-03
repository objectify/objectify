package com.google.gwt.user.client.rpc.core.com.googlecode.objectify.impl.ref;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.impl.ref.StdRef;

public class StdRef_ServerCustomFieldSerializer extends ServerCustomFieldSerializer<StdRef>
{

    @Override
    public void deserializeInstance(ServerSerializationStreamReader streamReader, StdRef instance,
            Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException
    {
        Key<?> key = (Key<?>) streamReader.readObject();
        Object value = streamReader.readObject();
        instance = new StdRef(key, value);
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, StdRef instance)
            throws SerializationException
    {
        Key<?> key = (Key<?>) streamReader.readObject();
        Object value = streamReader.readObject();
        instance = new StdRef(key, value);
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, StdRef instance)
            throws SerializationException
    {
        streamWriter.writeObject(instance.getKey());
        streamWriter.writeObject(instance.getValue());
    }
}
