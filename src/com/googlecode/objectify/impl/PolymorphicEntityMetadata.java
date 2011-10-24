package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.annotation.Subclass;


/**
 * The interface by which POJOs and datastore Entity objects are translated back and forth.
 * Subclasses implement specific mapping, including polymorphic mapping.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class PolymorphicEntityMetadata<T> implements EntityMetadata<T>
{
	/** Name of the out-of-band discriminator property in a raw Entity */
	public static final String DISCRIMINATOR_PROPERTY = "^d";
	
	/** Name of the list property which will hold all indexed discriminator values */
	public static final String DISCRIMINATOR_INDEX_PROPERTY = "^i";
	
	/** For every subclass, we maintain this info */
	static class SubclassInfo<V>
	{
		/** */
		public ConcreteEntityMetadata<V> metadata;

		/** 
		 * The discriminator for this subclass, or null for the base class.
		 */
		public String discriminator;

		/** 
		 * The discriminators that will be indexed for this subclass.  Empty for the base class or any
		 * subclasses for which all discriminators are unindexed.
		 */
		public List<String> indexedDiscriminators = new ArrayList<String>();

		/**
		 * @param discriminator can be null
		 */
		public SubclassInfo(ConcreteEntityMetadata<V> meta, String discriminator)
		{
			this.metadata = meta;
			this.discriminator = discriminator;
		}

		/**
		 * Recursively go through the class hierarchy adding any discriminators that are indexed
		 */
		public void addIndexedDiscriminators(Class<?> clazz)
		{
			if (clazz.isAnnotationPresent(com.googlecode.objectify.annotation.Entity.class) || clazz.isAnnotationPresent(javax.persistence.Entity.class))
				return;

			this.addIndexedDiscriminators(clazz.getSuperclass());
			
			Subclass sub = clazz.getAnnotation(Subclass.class);
			if (sub != null && !sub.unindexed())
			{
				String disc = (sub.name().length() > 0) ? sub.name() : clazz.getSimpleName();
				this.indexedDiscriminators.add(disc);
			}
		}
	}
	
	/** The metadata for the base @Entity, which has no discriminator */
	SubclassInfo<T> base;
	
	/** Keyed by discriminator value; doesn't include the base metdata */
	Map<String, ConcreteEntityMetadata<? extends T>> byDiscriminator = new HashMap<String, ConcreteEntityMetadata<? extends T>>();
	
	/** Keyed by Class, includes the base class */
	Map<Class<? extends T>, SubclassInfo<? extends T>> byClass = new HashMap<Class<? extends T>, SubclassInfo<? extends T>>();
	
	/**
	 * Initializes this metadata structure with the specified class.
	 * @param baseMetadata is the metadata for the @Entity class that defines the kind of the hierarchy
	 */
	public PolymorphicEntityMetadata(Class<T> clazz, ConcreteEntityMetadata<T> baseMetadata)
	{
		this.base = new SubclassInfo<T>(baseMetadata, null);
		
		this.byClass.put(clazz, this.base);
	}
	
	/**
	 * Registers a @Subclass in a polymorphic hierarchy.
	 * 
	 * @param clazz must have the @Subclass annotation
	 */
	public <S extends T> void addSubclass(Class<S> clazz, ConcreteEntityMetadata<S> subclassMeta)
	{
		Subclass sub = clazz.getAnnotation(Subclass.class);
		assert sub != null;
		
		String discriminator = (sub.name().length() > 0) ? sub.name() : clazz.getSimpleName();
		
		SubclassInfo<S> info = new SubclassInfo<S>(subclassMeta, discriminator);
		info.addIndexedDiscriminators(clazz);
		
		this.byClass.put(clazz, info);
		
		this.byDiscriminator.put(discriminator, subclassMeta);
		for (String alsoLoad: sub.alsoLoad())
			this.byDiscriminator.put(alsoLoad, subclassMeta);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getKind()
	 */
	public String getKind()
	{
		return this.base.metadata.getKind();
	}
	
	/**
	 * If the entity is null, return the metadata for the root entity of the polymorphic hierarchy.
	 * This will have the effect of making cache misses use the @Cached annotation of the @Entity.
	 *  
	 * @return the concrete entity metadata given the discriminator info 
	 */
	private EntityMetadata<? extends T> getConcrete(Entity ent)
	{
		if (ent == null)
			return this.base.metadata;

		String discriminator = (String)ent.getProperty(DISCRIMINATOR_PROPERTY);
		
		if (discriminator == null)
			return this.base.metadata;

		EntityMetadata<? extends T> sub = this.byDiscriminator.get(discriminator);
		if (sub == null)
			throw new IllegalStateException("No registered subclass for discriminator '" + discriminator + "'");
		else
			return sub;
	}
	
	/** @return the concrete entity metadata given the specific pojo */
	private <S extends T> SubclassInfo<S> getConcrete(S pojo)
	{
		@SuppressWarnings("unchecked")
		SubclassInfo<S> meta = (SubclassInfo<S>)this.byClass.get(pojo.getClass());
		if (meta != null)
			return meta;
		else
			throw new IllegalStateException("The class '" + pojo.getClass().getName() + "' was not registered");
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getCacheExpirySeconds()
	 */
	public Integer getCacheExpirySeconds()
	{
		return this.base.metadata.getCacheExpirySeconds();
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#toObject(com.google.appengine.api.datastore.Entity, com.googlecode.objectify.Objectify)
	 */
	public T toObject(Entity ent, Objectify ofy)
	{
		return this.getConcrete(ent).toObject(ent, ofy);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#toEntity(java.lang.Object, com.googlecode.objectify.Objectify)
	 */
	public Entity toEntity(T pojo, Objectify ofy)
	{
		SubclassInfo<T> info = this.getConcrete(pojo);

		Entity ent = info.metadata.toEntity(pojo, ofy);
		
		// Now put the discriminator value in entity
		if (info.discriminator != null)
			ent.setUnindexedProperty(DISCRIMINATOR_PROPERTY, info.discriminator);
		
		if (!info.indexedDiscriminators.isEmpty())
			ent.setProperty(DISCRIMINATOR_INDEX_PROPERTY, info.indexedDiscriminators);
		
		return ent;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#setKey(java.lang.Object, com.google.appengine.api.datastore.Key)
	 */
	public void setKey(T obj, com.google.appengine.api.datastore.Key key)
	{
		this.base.metadata.setKey(obj, key);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getRawKey(java.lang.Object)
	 */
	public com.google.appengine.api.datastore.Key getRawKey(Object obj)
	{
		return this.base.metadata.getRawKey(obj);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#isIdField(java.lang.String)
	 */
	public boolean isIdField(String propertyName)
	{
		return this.base.metadata.isIdField(propertyName);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#isNameField(java.lang.String)
	 */
	public boolean isNameField(String propertyName)
	{
		return this.base.metadata.isNameField(propertyName);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#hasParentField()
	 */
	public boolean hasParentField()
	{
		return this.base.metadata.hasParentField();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getEntityClass()
	 */
	@Override
	public Class<T> getEntityClass()
	{
		return this.base.metadata.getEntityClass();
	}
}
