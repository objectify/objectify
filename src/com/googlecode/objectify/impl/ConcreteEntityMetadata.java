package com.googlecode.objectify.impl;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.OnLoad;
import com.googlecode.objectify.annotation.OnSave;


/**
 * Knows how to map between datastore Entity objects and your typed POJO objects.
 * An instance of this class knows how to perform mapping of a specific subclass.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ConcreteEntityMetadata<T> implements EntityMetadata<T>
{
	/** */
	protected ObjectifyFactory fact;
	
	/** */
	protected Class<T> entityClass;

	/** */
	protected KeyMetadata<T> keyMetadata;

	/** Any methods in the hierarchy annotated with @OnSave, could be null */
	protected List<Method> onSaveMethods;

	/** Any methods in the hierarchy annotated with @OnLoad, could be null */
	protected List<Method> onLoadMethods;

	/** For translating between pojos and entities */
	protected Transmog<T> transmog;
	
	/** The cached annotation, or null if entity should not be cached */
	protected Cache cached;

	/**
	 * Inspects and stores the metadata for a particular entity class.
	 * @param clazz must be a properly-annotated Objectify entity class.
	 */
	public ConcreteEntityMetadata(ObjectifyFactory fact, Class<T> clazz)
	{
		this.fact = fact;
		this.entityClass = clazz;
		this.cached = clazz.getAnnotation(Cache.class);
		this.keyMetadata = new KeyMetadata<T>(fact, clazz);
		
		// Walk up the inheritance chain looking for @OnSave and @OnLoad
		this.processLifecycleCallbacks(clazz);
		
		// Now figure out how to handle normal properties
		this.transmog = new Transmog<T>(fact, clazz);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getCacheExpirySeconds()
	 */
	@Override
	public Integer getCacheExpirySeconds()
	{
		return this.cached == null ? null : this.cached.expirationSeconds();
	}
	
	/**
	 * Recursive function which walks up the superclass hierarchy looking
	 * for lifecycle-related methods (@OnSave and @OnLoad).
	 */
	private void processLifecycleCallbacks(Class<?> clazz)
	{
		if ((clazz == null) || (clazz == Object.class))
			return;

		// Start at the top of the chain
		this.processLifecycleCallbacks(clazz.getSuperclass());

		// Check all the methods
		for (Method method: clazz.getDeclaredMethods())
		{
			if (method.isAnnotationPresent(OnSave.class) || method.isAnnotationPresent(OnLoad.class))
			{
				method.setAccessible(true);
				
				Class<?>[] ptypes = method.getParameterTypes();
				
				for (int i=0; i<ptypes.length; i++)
					if (ptypes[i] != Objectify.class && ptypes[i] != Entity.class)
						throw new IllegalStateException("@OnSave and @OnLoad methods can only have parameters of type Objectify or Entity");
				
				if (method.isAnnotationPresent(OnSave.class))
				{
					if (this.onSaveMethods == null)
						this.onSaveMethods = new ArrayList<Method>();
					
					this.onSaveMethods.add(method);
				}
				
				if (method.isAnnotationPresent(OnLoad.class))
				{
					if (this.onLoadMethods == null)
						this.onLoadMethods = new ArrayList<Method>();
					
					this.onLoadMethods.add(method);
				}
			}
			
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#toObject(com.google.appengine.api.datastore.Entity, com.googlecode.objectify.Objectify)
	 */
	@Override
	public T toObject(Entity ent, Objectify ofy)
	{
		T pojo = fact.construct(this.entityClass);

		// This will set the id and parent fields as appropriate.
		keyMetadata.setKey(pojo, ent.getKey());

		this.transmog.load(ent, pojo);
		
		// If there are any @OnLoad methods, call them
		this.invokeLifecycleCallbacks(this.onLoadMethods, pojo, ent, ofy);

		return pojo;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#toEntity(java.lang.Object, com.googlecode.objectify.Objectify)
	 */
	@Override
	public Entity toEntity(T pojo, Objectify ofy)
	{
		Entity ent = keyMetadata.initEntity(pojo);

		// If there are any @OnSave methods, call them
		this.invokeLifecycleCallbacks(this.onSaveMethods, pojo, ent, ofy);
		
		this.transmog.save(pojo, ent);
		
		return ent;
	}
	
	/**
	 * Invoke a set of lifecycle callbacks on the pojo.
	 * 
	 * @param callbacks can be null if there are no callbacks
	 */
	private void invokeLifecycleCallbacks(List<Method> callbacks, Object pojo, Entity ent, Objectify ofy)
	{
		try
		{
			if (callbacks != null)
				for (Method method: callbacks)
					if (method.getParameterTypes().length == 0)
						method.invoke(pojo);
					else
					{
						Object[] params = new Object[method.getParameterTypes().length];
						for (int i=0; i<method.getParameterTypes().length; i++)
						{
							Class<?> ptype = method.getParameterTypes()[i];
							if (ptype == Objectify.class)
								params[i] = ofy;
							else if (ptype == Entity.class)
								params[i] = ent;
							else
								throw new IllegalStateException("Lifecycle callback cannot have parameter type " + ptype);
						}
						
						method.invoke(pojo, params);
					}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e)
		{
			if (e.getCause() instanceof RuntimeException)
				throw (RuntimeException)e.getCause();
			else
				throw new RuntimeException(e.getCause());
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getEntityClass()
	 */
	@Override
	public Class<T> getEntityClass()
	{
		return this.entityClass;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getKeyMetadata()
	 */
	@Override
	public KeyMetadata<T> getKeyMetadata()
	{
		return this.keyMetadata;
	}
}
