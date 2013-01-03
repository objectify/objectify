package com.google.gwt.user.client.rpc.core.com.googlecode.objectify.util;

import com.google.gwt.user.client.rpc.CustomFieldSerializer;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;
import com.googlecode.objectify.util.ResultNow;

public class ResultNow_CustomFieldSerializer extends CustomFieldSerializer<ResultNow>
{

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

    public static void deserialize(SerializationStreamReader streamReader, ResultNow instance)
            throws SerializationException
    {
        instance = new ResultNow(streamReader.readObject());
    }

    public static ResultNow instantiate(SerializationStreamReader streamReader) throws SerializationException
    {
        return new ResultNow(streamReader.readObject());
    }

    public static void serialize(SerializationStreamWriter streamWriter, ResultNow instance)
            throws SerializationException
    {
        streamWriter.writeObject(instance.now());
    }

}
