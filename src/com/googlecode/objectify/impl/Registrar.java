package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.EntitySubclass;

/**
 * <p>Maintains information about registered entity classes<p>
 * 
 * <p>There logic here is convoluted by polymorphic hierarchies.  Entity classes can
 * be registered in any particular order, requiring some considerable care.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Registrar
{
	/** */
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(Registrar.class.getName());
	
	/** Needed to obtain the converters */
	protected ObjectifyFactory fact;
	
	/** This maps kind to EntityMetadata */
	protected Map<String, EntityMetadata<?>> byKind = new HashMap<String, EntityMetadata<?>>();
	
	/** This maps class to EntityMetadata for all registered classes */
	protected Map<Class<?>, EntityMetadata<?>> byClass = new HashMap<Class<?>, EntityMetadata<?>>();
	
	/** True if any @Cached entities have been registered */
	protected boolean cacheEnabled;
	
	/** @return true if any entities are cacheable */
	public boolean isCacheEnabled()
	{
		return this.cacheEnabled;
	}
	
	/**
	 * @param fact is so that the conversions can be obtained
	 */
	public Registrar(ObjectifyFactory fact)
	{
		this.fact = fact;
	}
	
	/**
	 * <p>All POJO entity classes which are to be managed by Objectify
	 * must be registered first.  This method must be called in a single-threaded
	 * mode sometime around application initialization.  Re-registering a class
	 * has no effect.</p> 
	 * 
	 * @param clazz must be annotated with either @Entity or @EntitySubclass
	 */
	public <T> void register(Class<T> clazz)
	{
		// There are two possible cases
		// 1) This might be a simple class with @Entity
		// 2) This might be a class annotated with @EntitySubclass
		
		// If we are already registered, ignore
		if (this.byClass.containsKey(clazz))
			return;
		
		String kind = Key.getKind(clazz);
		
		if (clazz.isAnnotationPresent(Entity.class))
		{
			ConcreteEntityMetadata<T> cmeta = new ConcreteEntityMetadata<T>(this.fact, clazz);
			this.byKind.put(kind, cmeta);
			this.byClass.put(clazz, cmeta);
			
			if (cmeta.getCacheExpirySeconds() != null)
				this.cacheEnabled = true;
		}
		else if (clazz.isAnnotationPresent(EntitySubclass.class))
		{
			this.registerPolymorphicHierarchy(kind, clazz);
		}
		else
		{
			throw new IllegalArgumentException(clazz + " must be annotated with either @Entity or @EntitySubclass");
		}
	}
	
	/**
	 * Recursively register classes in the hierarchy which have @EntitySubclass
	 * or @Entity.  Stops when arriving at the first @Entity.  Safely handles
	 * classes that have already been registered, including @Entity classes
	 * that were registered as non-polymorphic.
	 * 
	 * It's damn near impossible to get the generics to line up properly, so
	 * we just use rawtypes in this method.  Forgive me.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected void registerPolymorphicHierarchy(String kind, Class clazz)
	{
		// If we are already registered, ignore
		if (this.byClass.containsKey(clazz))
			return;
		
		if (clazz == Object.class)
			throw new IllegalArgumentException("An @EntitySubclass hierarchy must have an @Entity superclass (direct or indirect)");
		
		// First thing we do is climb and take care of the actual root @Entity
		if (clazz.isAnnotationPresent(Entity.class))
		{
			this.register(clazz);
		}
		else
		{
			// First climb the hierarchy
			registerPolymorphicHierarchy(kind, clazz.getSuperclass());

			if (clazz.isAnnotationPresent(EntitySubclass.class))
			{
				// Populate this one way or another
				PolymorphicEntityMetadata polyMeta;
				
				// Since we climbed to @Entity first, there will always be something here, possibly a ConcreteEntityMetadata
				EntityMetadata meta = this.byKind.get(kind);
				
				if (meta instanceof ConcreteEntityMetadata)
				{
					polyMeta = new PolymorphicEntityMetadata(meta.getEntityClass(), (ConcreteEntityMetadata)meta);
					// reset the root byKind and byClass
					byKind.put(kind, polyMeta);
					byClass.put(meta.getEntityClass(), polyMeta);
				}
				else
				{
					polyMeta = (PolymorphicEntityMetadata)meta;
				}
				
				ConcreteEntityMetadata cmeta = new ConcreteEntityMetadata(this.fact, clazz);
				polyMeta.addSubclass(clazz, cmeta);
				byClass.put(clazz, polyMeta);
			}
		}
	}
	
	/**
	 * @return the metadata for the specified kind, or null if there was nothing appropriate registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadata(String kind)
	{
		return (EntityMetadata<T>)this.byKind.get(kind);
	}

	/**
	 * @return the metadata for the specified class, or null if there was nothing appropriate registered
	 */
	@SuppressWarnings("unchecked")
	public <T> EntityMetadata<T> getMetadata(Class<T> clazz)
	{
		return (EntityMetadata<T>)this.byClass.get(clazz);
	}
}