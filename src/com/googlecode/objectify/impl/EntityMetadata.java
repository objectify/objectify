package com.googlecode.objectify.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Id;
import javax.persistence.PostLoad;
import javax.persistence.PrePersist;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Cached;
import com.googlecode.objectify.annotation.Parent;


/**
 * Everything you need to know about mapping between Datastore Entity objects
 * and typed entity objects.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityMetadata<T>
{
	/** Needed for key translation */
	protected ObjectifyFactory factory;

	/** */
	protected Class<T> entityClass;
	protected Constructor<T> entityClassConstructor;

	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	protected String kind;

	/** We treat the @Id key field specially - it will be either Long id or String name */
	protected Field idField;
	protected Field nameField;

	/** If the entity has a @Parent field, treat it specially */
	protected Field parentField;
	
	/** Any methods in the hierarchy annotated with @PrePersist, could be null */
	protected List<Method> prePersistMethods;

	/** Any methods in the hierarchy annotated with @PostLoad, could be null */
	protected List<Method> postLoadMethods;

	/** For translating between pojos and entities */
	protected Transmog<T> transmog;
	
	/** The cached annotation, or null if entity should not be cached */
	protected Cached cached;

	/**
	 * Inspects and stores the metadata for a particular entity class.
	 * @param clazz must be a properly-annotated Objectify entity class.
	 */
	public EntityMetadata(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		this.entityClass = clazz;
		this.entityClassConstructor = TypeUtils.getNoArgConstructor(clazz);
		this.kind = this.factory.getKind(clazz);
		this.cached = clazz.getAnnotation(Cached.class);
		
		// Recursively walk up the inheritance chain looking for @Id and @Parent fields
		this.processKeyFields(clazz);

		// Walk up the inheritance chain looking for @PrePersist and @PostLoad
		this.processLifecycleCallbacks(clazz);
		
		// Now figure out how to handle normal properties
		this.transmog = new Transmog<T>(fact, clazz);
		
		// There must be some field marked with @Id
		if ((this.idField == null) && (this.nameField == null))
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + this.entityClass.getName());
	}

	/**
	 */
	public Class<T> getEntityClass()
	{
		return this.entityClass;
	}

	/** @return the datastore kind associated with this metadata */
	public String getKind()
	{
		return this.kind;
	}
	
	/**
	 * @return the Cached instruction for this entity, or null if it should not be cached.
	 */
	public Cached getCached()
	{
		return this.cached;
	}

	/**
	 * Recursive function which walks up the superclass hierarchy looking
	 * for key-related fields (@Id and @Parent).  Ignores all other fields;
	 * those are the responsibility of the Transmog.
	 */
	private void processKeyFields(Class<?> clazz)
	{
		if ((clazz == null) || (clazz == Object.class))
			return;

		// Start at the top of the chain
		this.processKeyFields(clazz.getSuperclass());

		// Check all the fields
		for (Field field: clazz.getDeclaredFields())
		{
			if (!TypeUtils.isSaveable(field))
				continue;

			field.setAccessible(true);

			if (field.isAnnotationPresent(Id.class))
			{
				if ((this.idField != null) || (this.nameField != null))
					throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + this.entityClass.getName());

				if ((field.getType() == Long.class) || (field.getType() == Long.TYPE))
					this.idField = field;
				else if (field.getType() == String.class)
					this.nameField = field;
				else
					throw new IllegalStateException("Only fields of type Long, long, or String are allowed as @Id. Invalid on field "
							+ field + " in " + clazz.getName());
			}
			else if (field.isAnnotationPresent(Parent.class))
			{
				if (this.parentField != null)
					throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + this.entityClass.getName());

				if (field.getType() != com.google.appengine.api.datastore.Key.class && field.getType() != Key.class)
					throw new IllegalStateException("Only fields of type Key<?> or Key are allowed as @Parent. Illegal parent '" + field + "' in " + clazz.getName());

				this.parentField = field;
			}
		}
	}

	/**
	 * Recursive function which walks up the superclass hierarchy looking
	 * for lifecycle-related methods (@PrePersist and @PostLoad).
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
			if (method.isAnnotationPresent(PrePersist.class) || method.isAnnotationPresent(PostLoad.class))
			{
				method.setAccessible(true);
				
				Class<?>[] ptypes = method.getParameterTypes();
				
				if (ptypes.length > 1 || (ptypes.length == 1 && !Entity.class.isAssignableFrom(ptypes[0])))
					throw new IllegalStateException("@PrePersist and @PostLoad methods can have at most one parameter of type Entity");
				
				if (method.isAnnotationPresent(PrePersist.class))
				{
					if (this.prePersistMethods == null)
						this.prePersistMethods = new ArrayList<Method>();
					
					this.prePersistMethods.add(method);
				}
				
				if (method.isAnnotationPresent(PostLoad.class))
				{
					if (this.postLoadMethods == null)
						this.postLoadMethods = new ArrayList<Method>();
					
					this.postLoadMethods.add(method);
				}
			}
			
		}
	}

	/**
	 * Converts an entity to an object of the appropriate type for this metadata structure.
	 * Does not check that the entity is appropriate; that should be done when choosing
	 * which EntityMetadata to call.
	 */
	public T toObject(Entity ent)
	{
		T pojo = TypeUtils.newInstance(this.entityClassConstructor);

		// This will set the id and parent fields as appropriate.
		this.setKey(pojo, ent.getKey());

		this.transmog.load(ent, pojo);
		
		// If there are any @PostLoad methods, call them
		this.invokeLifecycleCallbacks(this.postLoadMethods, pojo, ent);

		return pojo;
	}


	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity toEntity(T pojo)
	{
		Entity ent = this.initEntity(pojo);

		// If there are any @PrePersist methods, call them
		this.invokeLifecycleCallbacks(this.prePersistMethods, pojo, ent);
		
		this.transmog.save(pojo, ent);
		
		return ent;
	}
	
	/**
	 * Invoke a set of lifecycle callbacks on the pojo.
	 * 
	 * @param callbacks can be null if there are no callbacks
	 */
	private void invokeLifecycleCallbacks(List<Method> callbacks, Object pojo, Entity ent)
	{
		try
		{
			if (callbacks != null)
				for (Method method: callbacks)
					if (method.getParameterTypes().length == 0)
						method.invoke(pojo);
					else
						method.invoke(pojo, ent);
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
		catch (InvocationTargetException e) { throw new RuntimeException(e); }
	}

	/**
	 * <p>This hides all the messiness of trying to create an Entity from an object that:</p>
	 * <ul>
	 * <li>Might have a long id, might have a String name</li>
	 * <li>If it's a Long id, might be null and require autogeneration</li>
	 * <li>Might have a parent key</li>
	 * </ul>
	 * 
	 * @return an empty Entity object whose key has been set but no other properties.
	 */
	Entity initEntity(T obj)
	{
		try
		{
			com.google.appengine.api.datastore.Key parentKey = null;

			// First thing, get the parentKey (if appropriate)
			if (this.parentField != null)
			{
				parentKey = this.getRawKey(this.parentField, obj);
				if (parentKey == null)
					throw new IllegalStateException("Missing parent of " + obj);
			}

			if (this.idField != null)
			{
				Long id = (Long)this.idField.get(obj);	// possibly null
				if (id != null)
				{
					if (parentKey != null)
						return new Entity(KeyFactory.createKey(parentKey, this.kind, id));
					else
						return new Entity(KeyFactory.createKey(this.kind, id));
				}
				else // id is null, must autogenerate
				{
					if (parentKey != null)
						return new Entity(this.kind, parentKey);
					else
						return new Entity(this.kind);
				}
			}
			else	// this.nameField contains id
			{
				String name = (String)this.nameField.get(obj);
				if (name == null)
					throw new IllegalStateException("Tried to persist null String @Id for " + obj);

				if (parentKey != null)
					return new Entity(this.kind, name, parentKey);
				else
					return new Entity(this.kind, name);
			}
		}
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
	}

	/**
	 * Sets the relevant id and parent fields of the object to the values stored in the key.
	 * @param obj must be of the entityClass type for this metadata.
	 */
	public void setKey(T obj, com.google.appengine.api.datastore.Key key)
	{
		if (!this.entityClass.isAssignableFrom(obj.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + this.entityClass.getName() + " to set key of " + obj.getClass().getName());

		try
		{
			if (key.getName() != null)
			{
				if (this.nameField == null)
					throw new IllegalStateException("Loaded Entity has name but " + this.entityClass.getName() + " has no String @Id");

				this.nameField.set(obj, key.getName());
			}
			else
			{
				if (this.idField == null)
					throw new IllegalStateException("Loaded Entity has numeric id but " + this.entityClass.getName() + " has no Long (or long) @Id");

				this.idField.set(obj, key.getId());
			}

			com.google.appengine.api.datastore.Key parentKey = key.getParent();
			if (parentKey != null)
			{
				if (this.parentField == null)
					throw new IllegalStateException("Loaded Entity has parent but " + this.entityClass.getName() + " has no @Parent");

				if (this.parentField.getType() == com.google.appengine.api.datastore.Key.class)
					this.parentField.set(obj, parentKey);
				else
					this.parentField.set(obj, this.factory.rawKeyToTypedKey(parentKey));
			}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 * 
	 * @param obj must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if obj has a null id
	 */
	public com.google.appengine.api.datastore.Key getKey(Object obj)
	{
		if (!this.entityClass.isAssignableFrom(obj.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + this.entityClass.getName() + " to get key of " + obj.getClass().getName());

		try
		{
			if (this.nameField != null)
			{
				String name = (String)this.nameField.get(obj);

				if (this.parentField != null)
				{
					com.google.appengine.api.datastore.Key parent = this.getRawKey(this.parentField, obj);
					return KeyFactory.createKey(parent, this.kind, name);
				}
				else	// name yes parent no
				{
					return KeyFactory.createKey(this.kind, name);
				}
			}
			else	// has id not name
			{
				Long id = (Long) this.idField.get(obj);
				if (id == null)
					throw new IllegalArgumentException("You cannot create a Key for an object with a null @Id. Object was " + obj);

				if (this.parentField != null)
				{
					com.google.appengine.api.datastore.Key parent = this.getRawKey(this.parentField, obj);
					return KeyFactory.createKey(parent, this.kind, id);
				}
				else	// id yes parent no
				{
					return KeyFactory.createKey(this.kind, id);
				}
			}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/** @return the raw key even if the field is an Key */
	private com.google.appengine.api.datastore.Key getRawKey(Field keyField, Object obj) throws IllegalAccessException
	{
		if (keyField.getType() == com.google.appengine.api.datastore.Key.class)
			return (com.google.appengine.api.datastore.Key)keyField.get(obj);
		else
			return this.factory.typedKeyToRawKey((Key<?>)keyField.get(obj));
	}

	/**
	 * @return true if the property name corresponds to a Long/long @Id
	 *  field.  If the entity has a String name @Id, this will return false.
	 */
	public boolean isIdField(String propertyName)
	{
		return this.idField != null && this.idField.getName().equals(propertyName);
	}

	/**
	 * @return true if the property name corresponds to a String @Id
	 *  field.  If the entity has a Long/long @Id, this will return false.
	 */
	public boolean isNameField(String propertyName)
	{
		return this.nameField != null && this.nameField.getName().equals(propertyName);
	}

	/**
	 * @return true if the entity has a parent field
	 */
	public boolean hasParentField()
	{
		return this.parentField != null;
	}
}
