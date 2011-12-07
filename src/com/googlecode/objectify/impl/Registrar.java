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
	 * mode sometime around application initialization.</p> 
	 * 
	 * @param clazz must be annotated with either @Entity or @EntitySubclass
	 */
	public <T> void register(Class<T> clazz)
	{
		// TODO: make this a bit smarter some day so that re-registration of same item doesn't reparse everything.
		// However, that must be done carefully because sometimes you want to re-register something (say, after adding translators).
		
		// There are two possible cases
		// 1) This might be a simple class with @Entity
		// 2) This might be a class annotated with @EntitySubclass
		
		String kind = Key.getKind(clazz);
		
		if (clazz.isAnnotationPresent(EntitySubclass.class))
		{
			this.registerPolymorphicHierarchy(kind, clazz);
		}
		else if (clazz.isAnnotationPresent(Entity.class))
		{
			ConcreteEntityMetadata<T> cmeta = new ConcreteEntityMetadata<T>(this.fact, clazz);
			this.byKind.put(kind, cmeta);
			this.byClass.put(clazz, cmeta);
			
			if (cmeta.getCacheExpirySeconds() != null)
				this.cacheEnabled = true;
		}
		else
		{
			throw new IllegalArgumentException(clazz + " must be annotated with either @Entity or @EntitySubclass");
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
			throw new IllegalArgumentException("An @EntitySubclass hierarchy must have an @Entity superclass (direct or indirect)");
		
		// First thing we do is climb and take care of the actual root @Entity
		if (clazz.isAnnotationPresent(Entity.class))
		{
			// We're at the end of the recursion, deal with the base
			EntityMetadata<?> meta = this.byKind.get(kind);
			if (meta == null)
				meta = new ConcreteEntityMetadata<T>(this.fact, clazz);
			
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

			// We only register @EntitySubclass entities; other intermediate classes are not registered
			if (clazz.isAnnotationPresent(EntitySubclass.class) && !this.byClass.containsKey(clazz))
			{
				ConcreteEntityMetadata<T> cmeta = new ConcreteEntityMetadata<T>(this.fact, clazz);
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