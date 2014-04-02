package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.impl.Path;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphTranslator<P> extends NullSafeTranslator<P, PropertyContainer>
{
	/** Name of the out-of-band discriminator property in a raw Entity */
	public static final String DISCRIMINATOR_PROPERTY = "^d";

	/** Name of the list property which will hold all indexed discriminator values */
	public static final String DISCRIMINATOR_INDEX_PROPERTY = "^i";

	/** For every subclass, we maintain this info */
	static class SubclassInfo<V>
	{
		/** */
		public ClassTranslator<V> translator;

		/**
		 * The discriminator for this subclass, or null for the base class.
		 */
		public String discriminator;

		/**
		 * The discriminators that will be indexed for this subclass.  Empty for the base class or any
		 * subclasses for which all discriminators are unindexed.
		 */
		public List<String> indexedDiscriminators = new ArrayList<>();

		/**
		 * @param discriminator can be null to indicate the base class
		 */
		public SubclassInfo(ClassTranslator<V> translator, String discriminator) {
			this.translator = translator;
			this.discriminator = discriminator;
		}

		/**
		 * Recursively go through the class hierarchy adding any discriminators that are indexed
		 */
		public void addIndexedDiscriminators(Class<?> clazz) {
			if (clazz.isAnnotationPresent(com.googlecode.objectify.annotation.Entity.class))
				return;

			this.addIndexedDiscriminators(clazz.getSuperclass());

			EntitySubclass sub = clazz.getAnnotation(EntitySubclass.class);
			if (sub != null && sub.index()) {
				String disc = (sub.name().length() > 0) ? sub.name() : clazz.getSimpleName();
				this.indexedDiscriminators.add(disc);
			}
		}
	}

	/** The metadata for the base class, which has no discriminator */
	SubclassInfo<P> base;

	/** Keyed by discriminator value; doesn't include the base metdata */
	Map<String, ClassTranslator<?>> byDiscriminator = new HashMap<>();

	/** Keyed by Class, includes the base class */
	Map<Class<? extends P>, SubclassInfo<? extends P>> byClass = new HashMap<>();

	/**
	 * @param baseClass
	 * @param translator
	 */
	public PolymorphTranslator(Class<P> baseClass, ClassTranslator<P> translator) {

	}


	@Override
	protected P loadSafe(PropertyContainer node, LoadContext ctx, Path path, P into) throws SkipException {
		return null;
	}

	@Override
	protected PropertyContainer saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		return null;
	}
}
