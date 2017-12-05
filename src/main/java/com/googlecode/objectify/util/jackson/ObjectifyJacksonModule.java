package com.googlecode.objectify.util.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;

/**
 * Call jackson's {@code ObjectMapper.registerModule(new ObjectifyJacksonModule())} to enable
 * intelligent serialization and deserialization of various Objectify and GAE classes.
 */
public class ObjectifyJacksonModule extends SimpleModule {
	private static final long serialVersionUID = 1L;

	public ObjectifyJacksonModule() {
		super("Objectify", Version.unknownVersion());

		// Objectify Key
		this.addSerializer(Key.class, new KeySerializer());
		this.addKeySerializer(Key.class, new KeyKeySerializer());
		this.addDeserializer(Key.class, new KeyDeserializer());

		// Objectify Ref
		this.addSerializer(Ref.class, new RefSerializer());
		this.addKeySerializer(Ref.class, new RefKeySerializer());
		this.addDeserializer(Ref.class, new RefDeserializer());

		// Native datastore Key
		this.addSerializer(com.google.cloud.datastore.Key.class, new RawKeySerializer());
		this.addKeySerializer(com.google.cloud.datastore.Key.class, new RawKeyKeySerializer());
		this.addDeserializer(com.google.cloud.datastore.Key.class, new RawKeyDeserializer());
	}

}
