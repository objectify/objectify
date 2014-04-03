package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.google.common.base.Predicate;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.LogUtils;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * <p>Translator which translates entity classes, both root level and embedded. An entity class
 * is anything which has an @Entity in its superclass hierarchy.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityClassTranslator<P> extends AbstractClassTranslator<P>
{
	private static final Logger log = Logger.getLogger(EntityClassTranslator.class.getName());

	/** We don't want to include the key fields in normal population */
	private static final Predicate<Property> NON_KEY_FIELDS = new Predicate<Property>() {
		@Override
		public boolean apply(Property prop) {
			return prop.getAnnotation(Id.class) != null && prop.getAnnotation(Parent.class) != null;
		}
	};

	/**
	 * Metadata associated with the key.
	 */
	private KeyMetadata<P> keyMetadata;

	/**
	 */
	public EntityClassTranslator(Class<P> clazz, CreateContext ctx, Path path) {
		super(clazz, ctx, path, new ClassPopulator<P>(clazz, ctx, path, NON_KEY_FIELDS));

		// We should never have gotten this far in the registration process
		assert clazz.getAnnotation(Entity.class) != null;

		keyMetadata = new KeyMetadata<>(clazz, ctx, path);
	}

	/**
	 */
	public KeyMetadata<P> getKeyMetadata() {
		return keyMetadata;
	}

	/* */
	@Override
	public P loadSafe(PropertyContainer container, LoadContext ctx, Path path) throws SkipException {
		if (log.isLoggable(Level.FINEST))
			log.finest(LogUtils.msg(path, "Instantiating a " + clazz.getName()));

		P into = constructEmptyPojo(container, ctx, path);

		populator.load(container, ctx, path, into);

		return into;
	}

	/* */
	@Override
	public PropertyContainer saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		PropertyContainer into = constructEmptyContainer(pojo, path);

		populator.save(pojo, index, ctx, path, into);

		return into;
	}

	/**
	 */
	private PropertyContainer constructEmptyContainer(P pojo, Path path) {
		if (path.isRoot()) {
			return keyMetadata.initEntity(pojo);
		} else {
			EmbeddedEntity ent = new EmbeddedEntity();
			ent.setKey(keyMetadata.getRawKey(pojo));
			return ent;
		}
	}

	/**
	 */
	private P constructEmptyPojo(PropertyContainer container, LoadContext ctx, Path path) {
		P pojo = fact.construct(clazz);

		keyMetadata.setKey(pojo, DatastoreUtils.getKey(container), ctx, path);

		return pojo;
	}
}
