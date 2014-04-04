package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.google.appengine.api.blobstore.BlobKey;

import java.io.IOException;

/**
 * Will deserialize a BlobKey that was serialized with the BlobKeySerializer
 */
public class BlobKeyDeserializer extends StdDeserializer<BlobKey> {
	private static final long serialVersionUID = 1L;

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
