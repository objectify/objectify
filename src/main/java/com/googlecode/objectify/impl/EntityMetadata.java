package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
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
public class EntityMetadata<P>
{
	/** */
	private ObjectifyFactory fact;

	/** The base entity class type, ie the class with the @Entity annotation */
	private Class<P> entityClass;

	/** The cached annotation, or null if entity should not be cached */
	private Cache cached;

	/** */
	private Translator<P, PropertyContainer> translator;

	/**
	 * @param translator which can handle the root entity type.
	 */
	public EntityMetadata(ObjectifyFactory fact, Translator<P, PropertyContainer> translator) {
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
	public P load(Entity ent, LoadContext ctx) {
		return translator.load(ent, ctx, Path.root(), null);
	}

	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity save(P pojo, SaveContext ctx) {
		return (Entity)translator.save(pojo, false, ctx, Path.root());
	}

	/**
	 * Gets the class associated with this entity.
	 */
	public Class<P> getEntityClass() {
		return this.entityClass;
	}

	/**
	 * Get specific metadata about the key for this type.
	 */
	public KeyMetadata<P> getKeyMetadata();

	/**
=	 * @return the translator that will convert between native datastore representation and pojo for this type.
	 */
	public Translator<P, PropertyContainer> getTranslator() {
		return translator;
	}

}
