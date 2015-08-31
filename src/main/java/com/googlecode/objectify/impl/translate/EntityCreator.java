package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;


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
	public P load(PropertyContainer container, LoadContext ctx, Path path) throws SkipException {
		P pojo = construct(path);

		keyMetadata.setKey(pojo, container, ctx, path);

		return pojo;
	}

	@Override
	public PropertyContainer save(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		if (path.isRoot()) {
			return keyMetadata.initEntity(pojo);
		} else {
			EmbeddedEntity ent = new EmbeddedEntity();
			ent.setKey(keyMetadata.getRawKeyOrNull(pojo));
			return ent;
		}
	}
}
