package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.io.IOException;

/**
 * Configuring this serializer will make native datastore Key objects render as their web-safe string *when they are used as Map keys*.
 */
public class RawKeyKeySerializer extends JsonSerializer<Key> {

	@Override
	public void serialize(Key value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeFieldName(KeyFactory.keyToString(value));
	}
}
