package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.google.common.base.Predicate;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>Translator which maps classes, both normal embedded classes and Entity classes.</p>
 *
 * <p>Entity classes are just like any other class except they have @Id and @Parent fields and a kind.
 * When translating to native datastore structure (Entity for top level, EmbeddedEntity for an embedded field)
 * then these attributes are stored in the Key structure, not as properties.</p>
 *
 * <p>An entity class is any class which has the @Entity annotation anywhere in its superclass
 * hierarchy.</p>
 *
 * <p>Note that entities can be embedded in other objects; they are still entities. The
 * difference between an embedded class and an embedded entity is that the entity has a Key.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslatorFactory<P> implements TranslatorFactory<P, PropertyContainer>
{
	/** We don't want to include the key fields in normal population */
	private static final Predicate<Property> NON_KEY_FIELDS = new Predicate<Property>() {
		@Override
		public boolean apply(Property prop) {
			return prop.getAnnotation(Id.class) != null && prop.getAnnotation(Parent.class) != null;
		}
	};

	@Override
	public Translator<P, PropertyContainer> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		Class<P> clazz = (Class<P>)GenericTypeReflector.erase(type);

		Creator<P> creator;
		Populator<P> populator;

		// Entity is an inherited annotation; this checks up the hierarchy
		if (clazz.isAnnotationPresent(Entity.class)) {
			KeyMetadata<P> keyMetadata = new KeyMetadata<>(clazz, ctx, path);
			creator = new EntityCreator<>(clazz, ctx.getFactory(), keyMetadata);
			populator = new ClassPopulator<>(clazz, ctx, path, NON_KEY_FIELDS);
		} else {
			creator = new EmbeddedCreator<>(clazz, ctx.getFactory());
			populator = new ClassPopulator<P>(clazz, ctx, path);
		}

		ClassTranslator<P> classTranslator = new ClassTranslator<P>(clazz, path, creator, populator);

		if (clazz.isAnnotationPresent(Subclass.class))
			registerSubclass(classTranslator, clazz.getSuperclass(), annotations, ctx, path);

		return classTranslator;
	}

	/**
	 * Recursively register this subclass with all the superclass translators
	 */
	private void registerSubclass(ClassTranslator<P> translator, Class<? super P> superclass, Annotation[] annotations, CreateContext ctx, Path path) {
		if (superclass == Object.class)
			return;

		ClassTranslator<? super P> superTranslator = (ClassTranslator)ctx.getTranslator(superclass, annotations, ctx, path);
		superTranslator.registerSubclass(translator);

		registerSubclass(translator, superclass.getSuperclass(), annotations, ctx, path);
	}
}
