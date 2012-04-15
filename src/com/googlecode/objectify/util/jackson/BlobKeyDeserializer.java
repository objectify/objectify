package com.googlecode.objectify.util.jackson;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.appengine.api.blobstore.BlobKey;

/**
 * Will deserialize a BlobKey that was serialized with the BlobKeySerializer
 */
public class BlobKeyDeserializer extends StdDeserializer<BlobKey> {

	/** */
	public BlobKeyDeserializer() {
		super(BlobKey.class);
	}

	/** */
	@Override
	public BlobKey deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
		String keyStr = jp.getText();
		return new BlobKey(keyStr);
	}
}
