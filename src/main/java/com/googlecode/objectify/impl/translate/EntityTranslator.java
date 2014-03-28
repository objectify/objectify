package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.EntityMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Registrar;


/**
 * <p>Translator which can translate arbitrary entities based on registered kinds. This can be used to store
 * </p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityTranslator implements Translator<Object, PropertyContainer>
{
	private final Registrar registrar;

	/**
	 */
	public EntityTranslator(Registrar registrar) {
		this.registrar = registrar;
	}

	@Override
	public Object load(PropertyContainer node, LoadContext ctx) throws SkipException {
		Key key = getKey(node);
		EntityMetadata<?> meta = registrar.getMetadataSafe(key.getKind());

		return meta.getTranslator().load(node, ctx);
	}

	@Override
	public PropertyContainer save(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		EntityMetadata<Object> meta = (EntityMetadata<Object>)registrar.getMetadataSafe(pojo.getClass());

		return meta.getTranslator().save(pojo, index, ctx, path);
	}

	/**
	 */
	private Key getKey(PropertyContainer pc) {
		if (pc instanceof EmbeddedEntity)
			return ((EmbeddedEntity)pc).getKey();
		else if (pc instanceof Entity)
			return ((Entity)pc).getKey();
		else
			throw new IllegalArgumentException("Unknown new type of PropertyContainer");
	}

}
