package com.googlecode.objectify.impl;

import java.util.HashMap;
import java.util.Map;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Subclass;

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
	 * mode sometime around application initialization.</p> 
	 */
	public <T> void register(Class<T> clazz)
	{
		// There are two possible cases
		// 1) This might be a simple class with @Entity or unannotated
		// 2) This might be a class annotated with @Subclass
		
		String kind = Key.getKind(clazz);
		
		if (clazz.isAnnotationPresent(Subclass.class))
		{
			this.registerPolymorphicHierarchy(kind, clazz);
		}
		else
		{
			EntityMetadata<?> meta = this.byKind.get(kind);
			if (meta == null)
			{
				ConcreteEntityMetadata<T> cmeta = new ConcreteEntityMetadata<T>(this.fact.getConversions(), clazz);
				this.byKind.put(kind, cmeta);
				this.byClass.put(clazz, cmeta);
				
				if (cmeta.getCacheExpirySeconds() != null)
					this.cacheEnabled = true;
			}
			else if (meta instanceof PolymorphicEntityMetadata<?>)
			{
				// nothing special - the base class must already have been registered
			}
			else
			{
				throw new IllegalArgumentException("Attempted to register kind '" + kind + "' twice");
			}
		}
	}
	
	/**
	 * Recursively register classes in the hierarchy which have @Subclass
	 * or @Entity.  Stops when arriving at the first @Entity.  Safely handles
	 * classes that have already been registered, including @Entity classes
	 * that were registered as non-polymorphic.
	 * 
	 * @return the PolymorphicEntityMetadata associated with the kind.
	 */
	@SuppressWarnings("unchecked")
	protected <T> PolymorphicEntityMetadata<? super T> registerPolymorphicHierarchy(String kind, Class<T> clazz)
	{
		if (clazz == Object.class)
			throw new IllegalArgumentException("A @Subclass hierarchy must have an @Entity superclass (direct or indirect)");
		
		// First thing we do is climb and take care of the actual root @Entity
		if (clazz.isAnnotationPresent(Entity.class) || clazz.isAnnotationPresent(javax.persistence.Entity.class))
		{
			// We're at the end of the recursion, deal with the base
			EntityMetadata<?> meta = this.byKind.get(kind);
			if (meta == null)
				meta = new ConcreteEntityMetadata<T>(this.fact.getConversions(), clazz);
			
			if (meta instanceof ConcreteEntityMetadata<?>)
			{
				PolymorphicEntityMetadata<T> polymeta = new PolymorphicEntityMetadata<T>(clazz, (ConcreteEntityMetadata<T>)meta);
				this.byKind.put(kind, polymeta);
				this.byClass.put(clazz, polymeta);
				
				return polymeta;
			}
			else
				return (PolymorphicEntityMetadata<? super T>)meta;
		}
		else
		{
			// Climb the superclass tree, then check for subclass registration
			PolymorphicEntityMetadata<? super T> polymeta = this.registerPolymorphicHierarchy(kind, clazz.getSuperclass());

			// We only register @Subclass entities; other intermediate classes are not registered
			if (clazz.isAnnotationPresent(Subclass.class) && !this.byClass.containsKey(clazz))
			{
				ConcreteEntityMetadata<T> cmeta = new ConcreteEntityMetadata<T>(this.fact.getConversions(), clazz);
				polymeta.addSubclass(clazz, cmeta);
				this.byClass.put(clazz, polymeta);	// always the poly version when available
			}
			
			return polymeta;
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