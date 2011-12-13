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
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;


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
					if (ptypes[i] != Objectify.class && ptypes[i] != LoadContext.class && ptypes[i] != SaveContext.class)
						throw new IllegalStateException("@OnSave and @OnLoad methods can only have parameters of type Objectify, LoadContext, or SaveContext");
				
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
	public T load(Entity ent, final LoadContext ctx)
	{
		final T pojo = this.transmog.load(ent, ctx);
		
		// If there are any @OnLoad methods, call them after everything else
		ctx.defer(new Runnable() {
			@Override
			public void run() {
				invokeLifecycleCallbacks(onLoadMethods, pojo, ctx.getObjectify(), ctx, null);
			}
			
			@Override
			public String toString() {
				return "(deferred invoke lifecycle callbacks on " + pojo + ")";
			}
		});

		return pojo;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#toEntity(java.lang.Object, com.googlecode.objectify.Objectify)
	 */
	@Override
	public Entity save(T pojo, Objectify ofy)
	{
		SaveContext ctx = new SaveContext(ofy);
		
		// If there are any @OnSave methods, call them
		this.invokeLifecycleCallbacks(this.onSaveMethods, pojo, ofy, null, ctx);
		
		Entity ent = this.transmog.save(pojo, ctx);
		
		return ent;
	}
	
	/**
	 * Invoke a set of @OnLoad/@OnSave lifecycle callbacks on the pojo.
	 * 
	 * @param callbacks can be null if there are no callbacks
	 * @param lctx is the load context if this is a load operation
	 * @param sctx is the save context if this is a save operation
	 */
	private void invokeLifecycleCallbacks(List<Method> callbacks, Object pojo, Objectify ofy, LoadContext lctx, SaveContext sctx)
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
							else if (ptype == LoadContext.class)
								params[i] = lctx;
							else if (ptype == SaveContext.class)
								params[i] = sctx;
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
	public Class<T> getEntityClass() {
		return this.entityClass;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.EntityMetadata#getKeyMetadata()
	 */
	@Override
	public KeyMetadata<T> getKeyMetadata() {
		return this.transmog.getKeyMetadata();
	}

	/** Just for testing purposes; not part of the EntityMetadata interface */
	public Transmog<T> getTransmog() {
		return this.transmog;
	}
}
