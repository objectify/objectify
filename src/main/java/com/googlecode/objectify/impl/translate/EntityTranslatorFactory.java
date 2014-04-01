package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>Translator which can map @Entity objects. An @Entity is like any other class object except that
 * it has a key (@Id and @Parent fields). Entities can be standalone or embedded; if the path is
 * root this will produce an Entity, otherwise it will produce an EmbeddedEntity.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityTranslatorFactory<P> implements TranslatorFactory<P, PropertyContainer>
{
	@Override
	public Translator<P, PropertyContainer> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		Class<P> clazz = (Class<P>)GenericTypeReflector.erase(type);

		if (clazz.getAnnotation(Entity.class) == null && clazz.getAnnotation(EntitySubclass.class) == null)
			return null;

		return new EntityClassTranslator<P>(clazz, ctx, path);
	}
}
