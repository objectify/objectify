package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Registrar;
import lombok.RequiredArgsConstructor;


/**
 * <p>Translator which can translate arbitrary entities based on registered kinds. This provides a layer
 * of indirection, and allows you to store heterogeneous collections.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class EntityTranslator implements Translator<Object, FullEntity<?>>
{
	private final Registrar registrar;

	@Override
	public Object load(final Value<FullEntity<?>> container, final LoadContext ctx, final Path path) throws SkipException {
		final IncompleteKey key = container.get().getKey();
		final EntityMetadata<?> meta = registrar.getMetadataSafe(key.getKind());

		return meta.getTranslator().load(container, ctx, path);
	}

	@Override
	public Value<FullEntity<?>> save(final Object pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		@SuppressWarnings("unchecked")
		final EntityMetadata<Object> meta = (EntityMetadata<Object>)registrar.getMetadataSafe(pojo.getClass());

		return meta.getTranslator().save(pojo, index, ctx, path);
	}
}
