package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.cloud.datastore.Key;

import java.io.IOException;

/**
 * Will deserialize a google native datastore Key that was serialized with the RawKeySerializer
 */
public class RawKeyDeserializer extends StdDeserializer<Key> {
	private static final long serialVersionUID = 5025122822624438978L;

	/** */
	public RawKeyDeserializer() {
		super(Key.class);
	}

	/** */
	@Override
	public Key deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
		final String text = jp.getText();
		return Key.fromUrlSafe(text);
	}
}
