package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.google.cloud.datastore.Key;

import java.io.IOException;

/**
 * Configuring this serializer will make native datastore Key objects render as their web-safe string *when they are used as Map keys*.
 */
public class RawKeyKeySerializer extends JsonSerializer<Key> {

	@Override
	public void serialize(final Key value, final JsonGenerator jgen, final SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeFieldName(value.toUrlSafe());
	}
}
