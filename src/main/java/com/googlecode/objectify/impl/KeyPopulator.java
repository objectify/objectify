package com.googlecode.objectify.impl;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.Populator;
import com.googlecode.objectify.impl.translate.SaveContext;
import lombok.Getter;

/**
 * Populates the @Id/@Parent fields from an Entity key
 */
public class KeyPopulator<P> implements Populator<P> {
	/** */
	@Getter
	private final KeyMetadata<P> keyMetadata;

	public KeyPopulator(final Class<P> clazz, final CreateContext ctx, final Path path) {
		this.keyMetadata = new KeyMetadata<>(clazz, ctx, path);
	}

	@Override
	public void load(final FullEntity<?> container, final LoadContext ctx, final Path containerPath, final P intoPojo) {
		keyMetadata.setKey(intoPojo, container, ctx, containerPath);
	}

	@Override
	public void save(final P onPojo, boolean index, final SaveContext ctx, final Path containerPath, final FullEntity.Builder<?> into) {
		keyMetadata.setKey(into, onPojo);
	}
}