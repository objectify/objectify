package com.googlecode.objectify.impl;

import com.google.cloud.datastore.BaseEntity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.FullEntity.Builder;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.impl.translate.ClassTranslator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import lombok.Getter;


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
		this.keyMetadata = translator.getKeyMetadata();
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
			return translator.save(pojo, false, ctx, Path.root()).get();
		}
		catch (SaveException ex) { throw ex; }
		catch (Exception ex) {
			throw new SaveException(pojo, ex);
		}
	}
}
