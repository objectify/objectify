package com.google.gwt.user.client.rpc.core.com.googlecode.objectify.util;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.google.gwt.user.server.rpc.ServerCustomFieldSerializer;
import com.google.gwt.user.server.rpc.impl.DequeMap;
import com.google.gwt.user.server.rpc.impl.ServerSerializationStreamReader;
import com.googlecode.objectify.util.ResultNow;

public class ResultNow_ServerCustomFieldSerializer extends ServerCustomFieldSerializer<ResultNow>
{

    @Override
    public void deserializeInstance(ServerSerializationStreamReader streamReader, ResultNow instance,
            Type[] expectedParameterTypes, DequeMap<TypeVariable<?>, Type> resolvedTypes) throws SerializationException
    {
        instance = new ResultNow(streamReader.readObject());
    }

    @Override
    public void deserializeInstance(SerializationStreamReader streamReader, ResultNow instance)
            throws SerializationException
    {
        instance = new ResultNow(streamReader.readObject());
    }

    @Override
    public void serializeInstance(SerializationStreamWriter streamWriter, ResultNow instance)
            throws SerializationException
    {
        streamWriter.writeObject(instance.now());
    }

}
