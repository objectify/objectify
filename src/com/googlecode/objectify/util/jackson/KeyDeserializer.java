package com.googlecode.objectify.util.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.googlecode.objectify.Key;

/**
 * Will deserialize an Objectify Key<?> that was serialized with the KeySerializer
 */
@SuppressWarnings("rawtypes")
public class KeyDeserializer extends StdDeserializer<Key> {

	/** */
	public KeyDeserializer() {
		super(Key.class);
	}

	/** */
	@Override
	public Key deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String text = jp.getText();
		return Key.create(text);
	}
}
