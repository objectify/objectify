package com.googlecode.objectify.util.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

/**
 * Will deserialize a google native datastore Key that was serialized with the RawKeySerializer
 */
public class RawKeyDeserializer extends StdDeserializer<Key> {

	/** */
	public RawKeyDeserializer() {
		super(Key.class);
	}

	/** */
	@Override
	public Key deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String text = jp.getText();
		return KeyFactory.stringToKey(text);
	}
}
