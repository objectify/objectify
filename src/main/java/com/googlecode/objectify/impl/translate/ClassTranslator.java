package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.impl.Path;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * <p>Some common code for Translators which know how to convert a POJO type into a PropertiesContainer.
 * This might be polymorphic; we get polymorphism when @Subclasses are registered on this translator.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class ClassTranslator<P> extends NullSafeTranslator<P, PropertyContainer>
{
	/** Name of the out-of-band discriminator property in a PropertyContainer */
	public static final String DISCRIMINATOR_PROPERTY = "^d";

	/** Name of the list property which will hold all indexed discriminator values */
	public static final String DISCRIMINATOR_INDEX_PROPERTY = "^i";

	/** The declared class we are responsible for. */
	private final Class<P> declaredClass;

	/** Lets us construct the initial objects */
	private final Creator<P> creator;

	/** Does the heavy lifting of copying properties */
	private final Populator<P> populator;

	/**
	 * The discriminator for this subclass, or null for the base class.
	 */
	private final String discriminator;

	/**
	 * The discriminators that will be indexed for this subclass.  Empty for the base class or any
	 * subclasses for which all discriminators are unindexed.
	 */
	private final List<String> indexedDiscriminators = new ArrayList<>();

	/** Keyed by discriminator value, including alsoload discriminators */
	private Map<String, ClassTranslator<? extends P>> byDiscriminator = new HashMap<>();

	/** Keyed by Class, includes the base class */
	private Map<Class<? extends P>, ClassTranslator<? extends P>> byClass = new HashMap<>();

	/** */
	public ClassTranslator(final Class<P> declaredClass, final Path path, final Creator<P> creator, final Populator<P> populator) {
		log.trace("Creating class translator for {} at path '{}'", declaredClass.getName(), path);

		this.declaredClass = declaredClass;
		this.creator = creator;
		this.populator = populator;

		final Subclass sub = declaredClass.getAnnotation(Subclass.class);
		if (sub != null) {
			discriminator = (sub.name().length() > 0) ? sub.name() : declaredClass.getSimpleName();
			addIndexedDiscriminators(declaredClass);
		} else {
			discriminator = null;
		}
	}

	/**
	 * @return the class we translate
	 */
	public Class<P> getDeclaredClass() {
		return declaredClass;
	}

	/**
	 * @return the discriminator for this class, or null if this is not a @Subclass
	 */
	public String getDiscriminator() {
		return discriminator;
	}

	/**
	 * Get the populator associated with this class.
	 */
	public Populator<P> getPopulator() {
		return populator;
	}

	/**
	 * Get the creator associated with this class.
	 */
	public Creator<P> getCreator() { return creator; }

	/* */
	@Override
	public P loadSafe(PropertyContainer container, LoadContext ctx, Path path) throws SkipException {
		// check if we need to redirect to a different translator
		String containerDiscriminator = (String)container.getProperty(DISCRIMINATOR_PROPERTY);
		if (!Objects.equals(discriminator, containerDiscriminator)) {
			ClassTranslator<? extends P> translator = byDiscriminator.get(containerDiscriminator);
			if (translator == null) {
				throw new IllegalStateException("Datastore object has discriminator value '" + containerDiscriminator + "' but no relevant @Subclass is registered");
			} else {
				// This line fixes alsoLoad names in discriminators by changing the discriminator to what the
				// translator expects for loading that subclass. Otherwise we'll get the error above since the
				// translator discriminator and the container discriminator won't match.
				container.setUnindexedProperty(DISCRIMINATOR_PROPERTY, translator.getDiscriminator());

				return translator.load(container, ctx, path);
			}
		} else {
			// This is a normal load
			P into = creator.load(container, ctx, path);

			populator.load(container, ctx, path, into);

			return into;
		}
	}

	/* */
	@Override
	public PropertyContainer saveSafe(P pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
		// check if we need to redirect to a different translator
		if (pojo.getClass() != declaredClass) {
			// Sometimes generics are more of a hindrance than a help
			@SuppressWarnings("unchecked")
			ClassTranslator<P> translator = (ClassTranslator<P>)byClass.get(pojo.getClass());
			if (translator == null)
				throw new IllegalStateException("Class '" + pojo.getClass() + "' is not a registered @Subclass");
			else
				return translator.save(pojo, index, ctx, path);
		} else {
			// This is a normal save
			PropertyContainer into = creator.save(pojo, index, ctx, path);

			populator.save(pojo, index, ctx, path, into);

			if (discriminator != null) {
				into.setUnindexedProperty(DISCRIMINATOR_PROPERTY, discriminator);

				if (!indexedDiscriminators.isEmpty())
					into.setProperty(DISCRIMINATOR_INDEX_PROPERTY, indexedDiscriminators);
			}

			return into;
		}
	}

	/**
	 * Recursively go through the class hierarchy adding any discriminators that are indexed
	 */
	private void addIndexedDiscriminators(Class<?> clazz) {
		if (clazz == Object.class)
			return;

		this.addIndexedDiscriminators(clazz.getSuperclass());

		Subclass sub = clazz.getAnnotation(Subclass.class);
		if (sub != null && sub.index()) {
			String disc = (sub.name().length() > 0) ? sub.name() : clazz.getSimpleName();
			this.indexedDiscriminators.add(disc);
		}
	}

	/**
	 * Register a subclass translator with this class translator. That way if we get called upon
	 * to translate an instance of the subclass, we will forward to the correct translator.
	 */
	public void registerSubclass(ClassTranslator<? extends P> translator) {
		byDiscriminator.put(translator.getDiscriminator(), translator);

		Subclass sub = translator.getDeclaredClass().getAnnotation(Subclass.class);
		for (String alsoLoad: sub.alsoLoad())
			byDiscriminator.put(alsoLoad, translator);

		byClass.put(translator.getDeclaredClass(), translator);
	}

}
