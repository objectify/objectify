package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.FullEntity;
import com.googlecode.objectify.annotation.Subclass;
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
public class ClassTranslatorFactory<P> implements TranslatorFactory<P, FullEntity<?>>
{
	/** Cache of existing translators, see the class javadoc */
	private final Map<Class<P>, ClassTranslator<P>> translators = new HashMap<>();

	@Override
	public ClassTranslator<P> create(final TypeKey<P> tk, final CreateContext ctx, final Path path) {
		final Class<P> clazz = tk.getTypeAsClass();

		ClassTranslator<P> classTranslator = translators.get(clazz);
		if (classTranslator == null) {
			classTranslator = new ClassTranslator<>(clazz, ctx, path);

			translators.put(clazz, classTranslator);

			if (clazz.isAnnotationPresent(Subclass.class))
				registerSubclass(classTranslator, new TypeKey<>(clazz.getSuperclass(), tk), ctx, path);
		}

		return classTranslator;
	}

	/**
	 * Recursively register this subclass with all the superclass translators. This works because we cache
	 * translators uniquely in the factory.
	 */
	private void registerSubclass(final ClassTranslator<P> translator, final TypeKey<? super P> superclassTypeKey, final CreateContext ctx, final Path path) {
		if (superclassTypeKey.getTypeAsClass() == Object.class)
			return;

		@SuppressWarnings("unchecked")
		final ClassTranslator<? super P> superTranslator = create((TypeKey)superclassTypeKey, ctx, path);
		superTranslator.registerSubclass(translator);

		registerSubclass(translator, new TypeKey<>(superclassTypeKey.getTypeAsClass().getSuperclass()), ctx, path);
	}
}
