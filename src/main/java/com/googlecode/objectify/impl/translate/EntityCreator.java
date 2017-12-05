package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.PropertyContainer;


/**
 * <p>This version is for creating entity objects, which could be embedded or top-level.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityCreator<P> extends Creator<P>
{
	/**
	 * Metadata associated with the key.
	 */
	private KeyMetadata<P> keyMetadata;

	/**
	 */
	public EntityCreator(Class<P> clazz, Forge forge, KeyMetadata<P> keyMetadata) {
		super(clazz, forge);

		// We should never have gotten this far in the registration process
		assert clazz.getAnnotation(Entity.class) != null;

		this.keyMetadata = keyMetadata;
	}

	/** */
	public KeyMetadata<P> getKeyMetadata() {
		return keyMetadata;
	}

	@Override
	public P load(final PropertyContainer container, final LoadContext ctx, final Path path) throws SkipException {
		final P pojo = construct(path);

		keyMetadata.setKey(pojo, container, ctx, path);

		return pojo;
	}

	@Override
	public PropertyContainer save(final P pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		return keyMetadata.initPropertyContainer(pojo);
	}
}
