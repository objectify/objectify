package com.googlecode.objectify.impl;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.FullEntity.Builder;
import com.google.cloud.datastore.Value;
import com.google.common.collect.Lists;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.impl.translate.EntityCreator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import lombok.Getter;

import java.util.Collection;
import java.util.Map;


/**
 * Holds basic information about POJO entities, and can translate back and forth to the
 * datastore representation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityMetadata<P>
{
	/** The base entity class type, ie the class with the @Entity annotation */
	@Getter
	private final Class<P> entityClass;

	/** The cached annotation, or null if entity should not be cached */
	private final Cache cached;

	/**
	 * The translator that will convert between native datastore representation and pojo for this type.
	 */
	@Getter
	private final ClassTranslator<P> translator;

	/**
	 * Specific metadata about the key for this type.
	 */
	@Getter
	private final KeyMetadata<P> keyMetadata;

	/**
	 * @param clazz must have @Entity in its hierarchy
	 */
	public EntityMetadata(final ObjectifyFactory fact, final Class<P> clazz) {
		assert clazz.isAnnotationPresent(com.googlecode.objectify.annotation.Entity.class);

		this.entityClass = clazz;
		this.cached = clazz.getAnnotation(Cache.class);
		this.translator = (ClassTranslator<P>)fact.getTranslators().getRoot(clazz);
		this.keyMetadata = ((EntityCreator<P>)translator.getCreator()).getKeyMetadata();
	}

	/**
	 * Get the expiry associated with this kind, defined by the @Cached annotation.
	 * For polymorphic types, this is always the instruction on the root @Entity - you
	 * cannot provide per-type caching.
	 *
	 * @return null means DO NOT CACHE, 0 means "no limit", otherwise # of seconds
	 */
	public Integer getCacheExpirySeconds() {
		return this.cached == null ? null : this.cached.expirationSeconds();
	}

	/**
	 * Converts an entity to an object of the appropriate type for this metadata structure.
	 * Does not check that the entity is appropriate; that should be done when choosing
	 * which EntityMetadata to call.
	 */
	public P load(final BaseEntity<?> ent, final LoadContext ctx) {
		try {
			// The context needs to know the root entity for any given point
			ctx.setCurrentRoot(Key.create((com.google.cloud.datastore.Key)ent.getKey()));

			final EntityValue entityValue = makeLoadEntityValue(ent);
			return translator.load(entityValue, ctx, Path.root());
		}
		catch (LoadException ex) { throw ex; }
		catch (Exception ex) {
			throw new LoadException(ent, ex.getMessage(), ex);
		}
	}

	/**
	 * The problem is ProjectionEntity; there's no way to create an EntityValue with a ProjectionEntity
	 * so we can't use the standard translation system for {@code Value<Entity>}. Instead of making the
	 * translation API really complicated, just convert it to a FullEntity.
	 */
	private EntityValue makeLoadEntityValue(final BaseEntity<?> ent) {
		if (ent instanceof FullEntity<?>) {
			return EntityValue.of((FullEntity<?>)ent);
		} else {
			// Sadly there's no more graceful way of doing this
			final Builder<?> builder = FullEntity.newBuilder(ent.getKey());
			for (final String name : ent.getNames()) {
				final Value<?> value = ent.getValue(name);
				builder.set(name, value);
			}

			return EntityValue.of(builder.build());
		}
	}

	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public FullEntity<?> save(final P pojo, final SaveContext ctx) {
		try {
			ctx.startOneEntity();

			final FullEntity<?> ent = translator.save(pojo, false, ctx, Path.root()).get();
			return createSyntheticIndexes(ent, ctx);
		}
		catch (SaveException ex) { throw ex; }
		catch (Exception ex) {
			throw new SaveException(pojo, ex.getMessage(), ex);
		}
	}

	/**
	 * Establish any synthetic dot-separate indexes for embedded things that are indexed.
	 */
	private FullEntity<?> createSyntheticIndexes(final FullEntity<?> entity, final SaveContext ctx) {

		// The datastore rejects our synthetic x.y.z indexes, possibly because it creates its own. It appears to
		// be undocumented, needs to run some tests.
		// TODO decide whether we can throw out this whole function
		if (true) return entity;

		// Maybe we can just leave it as-is
		if (ctx.getIndexes().keySet().stream().noneMatch(Path::isEmbedded)) {
			return entity;
		}

		final Builder<?> withIndexes = FullEntity.newBuilder(entity);

		// Look for anything which is embedded and therefore won't be automatically indexed
		for (final Map.Entry<Path, Collection<Value<?>>> index: ctx.getIndexes().entrySet()) {
			final Path path = index.getKey();
			final Collection<Value<?>> values = index.getValue();

			if (path.isEmbedded()) {
				// Need to copy the values list otherwise it will clear when we reset the context indexes
				withIndexes.set(path.toPathString(), Lists.newArrayList(values));
			}
		}

		return withIndexes.build();
	}

}
