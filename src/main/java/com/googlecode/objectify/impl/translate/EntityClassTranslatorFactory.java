package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>Translator which maps @Entity classes. Entity classes are just like any other class
 * except they have @Id and @Parent fields and a kind. When translating to native datastore
 * structure (Entity for top level, EmbeddedEntity for an embedded field) then these attributes
 * are stored in the Key structure, not as properties.</p>
 *
 * <p>An entity class is any class which has the @Entity annotation anywhere in its superclass
 * hierarchy.</p>
 *
 * <p>Note that entities can be embedded in other objects; they are still entities. The
 * difference between an embedded class and an embedded entity is that the entity has a Key.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityClassTranslatorFactory<P> implements TranslatorFactory<P, PropertyContainer>
{
	@Override
	public Translator<P, PropertyContainer> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		Class<P> clazz = (Class<P>)GenericTypeReflector.erase(type);

		// Entity is an inherited annotation
		if (!clazz.isAnnotationPresent(Entity.class))
			return null;

		return new EntityClassTranslator<P>(clazz, ctx, path);
	}

}
