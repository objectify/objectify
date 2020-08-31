package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.IncompleteKey;
import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.google.common.base.Preconditions;
import com.googlecode.objectify.annotation.Subclass;
import com.googlecode.objectify.impl.Forge;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.LogUtils;
import lombok.Getter;
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
public class ClassTranslator<P> extends NullSafeTranslator<P, FullEntity<?>>
{
	/** Name of the out-of-band discriminator property in a PropertyContainer */
	public static final String DISCRIMINATOR_PROPERTY = "^d";

	/** Name of the list property which will hold all indexed discriminator values */
	public static final String DISCRIMINATOR_INDEX_PROPERTY = "^i";

	/** The declared class we are responsible for. */
	@Getter
	private final Class<P> declaredClass;

	/** Lets us construct the initial objects */
	@Getter
	private final Forge forge;

	/** Does the heavy lifting of copying properties */
	@Getter
	private final ClassPopulator<P> populator;

	/**
	 * The discriminator for this subclass, or null if this is not a @Subclass
	 */
	@Getter
	private final String discriminator;

	/**
	 * The discriminators that will be indexed for this subclass.  Empty for the base class or any
	 * subclasses for which all discriminators are unindexed.
	 */
	private final List<StringValue> indexedDiscriminators = new ArrayList<>();

	/** Keyed by discriminator value, including alsoload discriminators */
	private Map<String, ClassTranslator<? extends P>> byDiscriminator = new HashMap<>();

	/** Keyed by Class, includes the base class */
	private Map<Class<? extends P>, ClassTranslator<? extends P>> byClass = new HashMap<>();

	/** */
	public ClassTranslator(final Class<P> declaredClass, final CreateContext ctx, final Path path) {
		log.trace("Creating class translator for {} at path '{}'", declaredClass.getName(), path);

		this.declaredClass = declaredClass;
		this.forge = ctx.getFactory();
		this.populator = new ClassPopulator<>(declaredClass, ctx, path);

		final Subclass sub = declaredClass.getAnnotation(Subclass.class);
		if (sub != null) {
			discriminator = (sub.name().length() > 0) ? sub.name() : declaredClass.getSimpleName();
			addIndexedDiscriminators(declaredClass);
		} else {
			discriminator = null;
		}
	}

	/* */
	@Override
	public P loadSafe(final Value<FullEntity<?>> container, final LoadContext ctx, final Path path) throws SkipException {
		Preconditions.checkArgument(container.get() instanceof FullEntity, "Expected type %s at path '%s' but instead found %s", ValueType.ENTITY, path, container.getType());

		// check if we need to redirect to a different translator
		final String containerDiscriminator = container.get().contains(DISCRIMINATOR_PROPERTY) ? container.get().getString(DISCRIMINATOR_PROPERTY) : null;	// wow no Optional or nullable get
		if (!Objects.equals(discriminator, containerDiscriminator)) {
			final ClassTranslator<? extends P> translator = byDiscriminator.get(containerDiscriminator);
			if (translator == null) {
				throw new IllegalStateException("Datastore object has discriminator value '" + containerDiscriminator + "' but no relevant @Subclass is registered");
			} else {
				// This fixes alsoLoad names in discriminators by changing the discriminator to what the
				// translator expects for loading that subclass. Otherwise we'll get the error above since the
				// translator discriminator and the container discriminator won't match.
				final StringValue discriminatorValue = StringValue.newBuilder(translator.getDiscriminator()).setExcludeFromIndexes(true).build();
				final FullEntity<?> updatedEntity = FullEntity.newBuilder(container.get()).set(DISCRIMINATOR_PROPERTY, discriminatorValue).build();
				return translator.load(EntityValue.of(updatedEntity), ctx, path);
			}
		} else {
			// This is a normal load
			if (log.isTraceEnabled())
				log.trace(LogUtils.msg(path, "Instantiating a " + declaredClass.getName()));

			final P into = forge.construct(declaredClass);

			populator.load(container.get(), ctx, path, into);

			return into;
		}
	}

	/* */
	@Override
	public Value<FullEntity<?>> saveSafe(final P pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
		// check if we need to redirect to a different translator
		if (pojo.getClass() != declaredClass) {
			// Sometimes generics are more of a hindrance than a help
			@SuppressWarnings("unchecked")
			final ClassTranslator<P> translator = (ClassTranslator<P>)byClass.get(pojo.getClass());
			if (translator == null)
				throw new IllegalStateException("Class '" + pojo.getClass() + "' is not a registered @Subclass");
			else
				return translator.save(pojo, index, ctx, path);
		} else {
			// This is a normal save
			final FullEntity.Builder<IncompleteKey> into = FullEntity.newBuilder();

			populator.save(pojo, index, ctx, path, into);

			if (discriminator != null) {
				into.set(DISCRIMINATOR_PROPERTY, StringValue.newBuilder(discriminator).setExcludeFromIndexes(true).build());

				if (!indexedDiscriminators.isEmpty())
					into.set(DISCRIMINATOR_INDEX_PROPERTY, ListValue.of(indexedDiscriminators));
			}

			// The question of whether to index this is weird. In order for subthings to be indexed, the entity needs
			// to be indexed. But then lists with index-heterogeous values (say, nulls) get reordered (!)
			// by the datastore. So we always index EntityValues and force all the list translators homogenize their lists.
			// Gross but seems to be the only way.
			return EntityValue.of(into.build());
		}
	}

	/**
	 * Recursively go through the class hierarchy adding any discriminators that are indexed
	 */
	private void addIndexedDiscriminators(final Class<?> clazz) {
		if (clazz == Object.class)
			return;

		this.addIndexedDiscriminators(clazz.getSuperclass());

		final Subclass sub = clazz.getAnnotation(Subclass.class);
		if (sub != null && sub.index()) {
			final String disc = (sub.name().length() > 0) ? sub.name() : clazz.getSimpleName();
			this.indexedDiscriminators.add(StringValue.of(disc));
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

	/** Only works on @Entity classes */
	public KeyMetadata<P> getKeyMetadata() {
		return populator.getKeyMetadata();
	}
}
