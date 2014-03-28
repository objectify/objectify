package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;


/**
 * Holds basic information about POJO entities, and can translate back and forth to the
 * datastore representation.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityMetadata<T>
{
	/** */
	protected ObjectifyFactory fact;

	/** The base entity class type, ie the class with the @Entity annotation */
	protected Class<T> entityClass;

	/** The cached annotation, or null if entity should not be cached */
	protected Cache cached;

	/** */
	Translator<T> translator;

	/**
	 * @param translator which can handle the root entity type.
	 */
	public EntityMetadata(ObjectifyFactory fact, Translator<T> translator) {
		this.fact = fact;
		this.translator = translator;
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
	public T load(Entity ent, LoadContext ctx) {
		final T pojo = this.transmog.load(ent, ctx);

		// If there are any @OnLoad methods, call them after everything else
		ctx.defer(new Runnable() {
			@Override
			public void run() {
				invokeLifecycleCallbacks(onLoadMethods, pojo, ctx.getLoader().getObjectify(), ctx, null);
			}

			@Override
			public String toString() {
				return "(deferred invoke lifecycle callbacks on " + pojo + ")";
			}
		});

		return pojo;
	}

	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity save(T pojo, SaveContext ctx) {
		return (Entity)translator.save(pojo, false, ctx, Path.root());
	}


	/**
	 * Gets the class associated with this entity.
	 */
	public Class<T> getEntityClass() {
		return this.entityClass;
	}

	/**
	 * Get specific metadata about the key for this type.
	 */
	public KeyMetadata<T> getKeyMetadata();
}
