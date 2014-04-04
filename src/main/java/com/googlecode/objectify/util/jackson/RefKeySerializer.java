package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.googlecode.objectify.Ref;

import java.io.IOException;

/**
 * Like RefSerializer, but handles Refs when they are used as Map keys.  Always serializes to the key string representation.
 */
@SuppressWarnings("rawtypes")
public class RefKeySerializer extends JsonSerializer<Ref> {

	@Override
	public void serialize(Ref value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
		jgen.writeFieldName(value.key().getString());
	}
}
