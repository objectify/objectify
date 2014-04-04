package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.googlecode.objectify.Key;

import java.io.IOException;

/**
 * Will deserialize an Objectify Key<?> that was serialized with the KeySerializer
 */
@SuppressWarnings("rawtypes")
public class KeyDeserializer extends StdDeserializer<Key> {
	private static final long serialVersionUID = 1L;

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
