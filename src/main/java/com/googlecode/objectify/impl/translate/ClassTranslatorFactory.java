package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;

import java.util.HashMap;
import java.util.Map;


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
 * <p>One noteworthy issue is that we must ensure there is only one classtranslator for any
 * given class. Normally the discovery process creates a separate translator for each set of
 * annotations, however, this screws up the subclass registration process, which needs to
 * register at each parent class translator. Since field annotations are actually irrelevant to
 * the internal function of a ClassTranslator, we can just cache class translators here
 * in the factory. There will never be more than one translator for a given class, even
 * though many TypeKeys may point at it.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ClassTranslatorFactory<P> implements TranslatorFactory<P, PropertyContainer>
{
	/** Cache of existing translators, see the class javadoc */
	private Map<Class<P>, ClassTranslator<P>> translators = new HashMap<>();

	@Override
	public ClassTranslator<P> create(TypeKey<P> tk, CreateContext ctx, Path path) {
		Class<P> clazz = tk.getTypeAsClass();

		ClassTranslator<P> classTranslator = translators.get(clazz);
		if (classTranslator == null) {
			// Entity is an inherited annotation; this checks up the hierarchy
			classTranslator = (clazz.isAnnotationPresent(Entity.class))
					? createEntityClassTranslator(clazz, ctx, path)
					: createEmbeddedClassTranslator(clazz, ctx, path);

			translators.put(clazz, classTranslator);

			if (clazz.isAnnotationPresent(Subclass.class))
				registerSubclass(classTranslator, new TypeKey<>(clazz.getSuperclass(), tk), ctx, path);
		}

		return classTranslator;
	}

	/**
	 */
	public static <P> ClassTranslator<P> createEntityClassTranslator(Class<P> clazz, CreateContext ctx, Path path) {
		KeyMetadata<P> keyMetadata = new KeyMetadata<>(clazz, ctx, path);
		Creator<P> creator = new EntityCreator<>(clazz, ctx.getFactory(), keyMetadata);
		Populator<P> populator = new ClassPopulator<>(clazz, ctx, path);

		return new ClassTranslator<>(clazz, path, creator, populator);
	}

	/**
	 */
	public static <P> ClassTranslator<P> createEmbeddedClassTranslator(Class<P> clazz, CreateContext ctx, Path path) {
		Creator<P> creator = new EmbeddedCreator<>(clazz, ctx.getFactory());
		Populator<P> populator = new ClassPopulator<>(clazz, ctx, path);

		return new ClassTranslator<>(clazz, path, creator, populator);
	}

	/**
	 * Recursively register this subclass with all the superclass translators. This works because we cache
	 * translators uniquely in the factory.
	 */
	private void registerSubclass(ClassTranslator<P> translator, TypeKey<? super P> superclassTypeKey, CreateContext ctx, Path path) {
		if (superclassTypeKey.getTypeAsClass() == Object.class)
			return;

		@SuppressWarnings("unchecked")
		ClassTranslator<? super P> superTranslator = create((TypeKey)superclassTypeKey, ctx, path);
		superTranslator.registerSubclass(translator);

		registerSubclass(translator, new TypeKey<>(superclassTypeKey.getTypeAsClass().getSuperclass()), ctx, path);
	}
}
