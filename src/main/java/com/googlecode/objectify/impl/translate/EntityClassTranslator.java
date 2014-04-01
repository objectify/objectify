package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslatableProperty;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.DatastoreUtils;

import java.lang.reflect.Type;


/**
 * <p>Translator which translates entities, both root level and embedded.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityClassTranslator<P> extends ClassTranslator<P>
{
	/** The @Id field on the pojo - it will be Long, long, or String. Just a temporary holder for keymetadata. */
	private TranslatableProperty<Object, Object> idMeta;

	/** The @Parent field on the pojo, or null if there is no parent. Just a temporary holder for keymetadata. */
	private TranslatableProperty<Object, Object> parentMeta;

	/**
	 * Metadata associated with the key.
	 */
	private KeyMetadata<P> keyMetadata;

	/**
	 */
	@SuppressWarnings("unchecked")
	public EntityClassTranslator(Type type, CreateContext ctx, Path path) {
		super((Class<P>)GenericTypeReflector.erase(type), ctx, path);

		// We should never have gotten this far in the registration process
		assert clazz.getAnnotation(Entity.class) != null || clazz.getAnnotation(EntitySubclass.class) != null;

		// Now that we have idMeta and parentMeta, we can construct the keyMetadata
		keyMetadata = new KeyMetadata<>(clazz, idMeta, parentMeta, ctx);
	}

	/**
	 * Look for the id and parent properties
	 *
	 * @return false if we get a special id/parent property so that the field doesn't get processed as a normal prop.
	 */
	@Override
	protected boolean consider(TranslatableProperty<Object, Object> tprop) {

		Property prop = tprop.getProperty();

		if (prop.getAnnotation(Id.class) != null) {
			if (this.idMeta != null)
				throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + clazz.getName());

			if ((prop.getType() != Long.class) && (prop.getType() != long.class) && (prop.getType() != String.class))
				throw new IllegalStateException("@Id field '" + prop.getName() + "' in " + clazz.getName() + " must be of type Long, long, or String");

			this.idMeta = tprop;

			return false;
		} else if (prop.getAnnotation(Parent.class) != null) {
			if (this.parentMeta != null)
				throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + clazz.getName());

			if (!isAllowedParentFieldType(prop.getType()))
				throw new IllegalStateException("@Parent fields must be Ref<?>, Key<?>, or datastore Key. Illegal parent: " + prop);

			this.parentMeta = tprop;

			return false;
		} else {
			return true;
		}
	}

	/** @return true if the type is an allowed parent type */
	private boolean isAllowedParentFieldType(Type type) {

		Class<?> erased = GenericTypeReflector.erase(type);

		return com.google.appengine.api.datastore.Key.class.isAssignableFrom(erased)
				|| Key.class.isAssignableFrom(erased)
				|| Ref.class.isAssignableFrom(erased);
	}

	@Override
	protected PropertyContainer constructEmptyContainer(P pojo, Path path) {
		if (path.isRoot()) {
			return keyMetadata.initEntity(pojo);
		} else {
			EmbeddedEntity ent = new EmbeddedEntity();
			ent.setKey(keyMetadata.getRawKey(pojo));
			return ent;
		}
	}

	@Override
	protected P constructEmptyPojo(PropertyContainer container, LoadContext ctx, Path path) {
		P pojo = super.constructEmptyPojo(container, ctx, path);

		keyMetadata.setKey(pojo, DatastoreUtils.getKey(container), ctx, path);

		return pojo;
	}
}
