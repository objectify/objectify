package com.googlecode.objectify;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.annotation.Parent;


/**
 * Everything you need to know about mapping between Datastore Entity objects
 * and typed entity objects.
 *  
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityMetadata
{
	/** We do not persist fields with any of these modifiers */
	static final int BAD_MODIFIERS = Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT;
	
	/** We need to be able to populate fields and methods with @OldName */
	static interface Populator
	{
		/** Actually populate the thing (field or method) */
		void populate(Object entity, Object value);
		/** Get the thing for hashing, string conversion, etc */
		Object getThing();
		/** Get the type of the thing */
		Class<?> getType();
	}
	
	/** Works with fields */
	static class FieldPopulator implements Populator
	{
		Field field;
		public FieldPopulator(Field field) { this.field = field; }
		public Object getThing() { return this.field; }
		public Class<?> getType() { return this.field.getType(); }
		public void populate(Object entity, Object value)
		{
			try { this.field.set(entity, value); }
			catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
		}
	}
	
	/** Works with methods */
	static class MethodPopulator implements Populator
	{
		Method method;
		public MethodPopulator(Method method) { this.method = method; }
		public Object getThing() { return this.method; }
		public Class<?> getType() { return this.method.getParameterTypes()[0]; }
		public void populate(Object entity, Object value)
		{
			try { this.method.invoke(entity, value); }
			catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
			catch (InvocationTargetException ex) { throw new RuntimeException(ex); }
		}
	}
	
	/** */
	private Class<?> entityClass;
	public Class<?> getEntityClass() { return this.entityClass; }
	
	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	private String kind;
	
	/** We treat the @Id key field specially - it will be either Long id or String name */
	private Field idField;
	private Field nameField;
	
	/** If the entity has a @Parent field, treat it specially */
	private Field parentField;
	
	/** The fields we persist, not including the @Id or @Parebnt fields */
	private Set<Field> writeables = new HashSet<Field>();
	
	/** The things that we read, keyed by name (including @OldName fields and methods).  A superset of writeables. */
	private Map<String, Populator> readables = new HashMap<String, Populator>();
	
	/** */
	public EntityMetadata(Class<?> clazz)
	{
		this.entityClass = clazz;
		this.kind = ObjectifyFactory.getKind(clazz);
		
		// Recursively walk up the inheritance chain looking for fields
		this.visit(clazz);
		
		// There must be some field marked with @Id
		if (this.idField == null && this.nameField == null)
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + this.entityClass.getName());
	}
	
	/**
	 * Recursive function adds any appropriate fields to our internal data
	 * structures for persisting and retreiving later.
	 */
	private void visit(Class<?> clazz)
	{
		if (clazz == null || clazz == Object.class)
			return;
		
		// Check all the fields
		for (Field field: clazz.getDeclaredFields())
		{
			if (field.isAnnotationPresent(Transient.class) || (field.getModifiers() & BAD_MODIFIERS) != 0)
				continue;
			
			field.setAccessible(true);
			
			if (field.isAnnotationPresent(Id.class))
			{
				if (this.idField != null || this.nameField != null)
					throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + this.entityClass.getName());
					
				if (field.getType() == Long.class || field.getType() == Long.TYPE)
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
				
				if (field.getType() != Key.class)
					throw new IllegalStateException("Only fields of type Key are allowed as @Parent. Illegal parent '" + field + "' in " + clazz.getName());
				
				this.parentField = field;
			}
			else
			{
				this.writeables.add(field);
				
				Populator pop = new FieldPopulator(field);
				this.putReadable(field.getName(), pop);
				
				OldName old = field.getAnnotation(OldName.class);
				if (old != null)
					this.putReadable(old.value(), pop);
			}
		}
		
		// Now look for methods with one param that are annotated with @OldName
		for (Method method: clazz.getDeclaredMethods())
		{
			OldName oldName = method.getAnnotation(OldName.class);
			if (oldName != null)
			{
				if (method.getParameterTypes().length != 1)
					throw new IllegalStateException("@OldName methods must have a single parameter. Can't use " + method);

				method.setAccessible(true);
				
				this.putReadable(oldName.value(), new MethodPopulator(method));
			}
		}
	}
	
	/**
	 * Adds the key/value pair to this.readables, throwing an exception if there
	 * is a duplicate key.
	 */
	private void putReadable(String name, Populator p)
	{
		if (this.readables.put(name, p) != null)
			throw new IllegalStateException(
					"Data property name '" + name + "' is duplicated in hierarchy of " + 
					this.entityClass.getName() + ". Check for conflicting @OldNames.");
	}
	
	/**
	 * Converts an entity to an object of the appropriate type for this metadata structure.
	 * Does not check that the entity is appropriate; that should be done when choosing
	 * which EntityMetadata to call.
	 */
	public Object toObject(Entity ent)
	{
		try
		{
			Object obj = this.entityClass.newInstance();

			// This will set the id and parent fields as appropriate.
			this.setKey(obj, ent.getKey());

			// Keep track of which fields and methods have been done so we don't repeat any;
			// this could happen if an Entity has data for both @OldName and the current name.
			Set<Object> done = new HashSet<Object>();
			
			for (Map.Entry<String, Object> property: ent.getProperties().entrySet())
			{
				Populator pop = this.readables.get(property.getKey());
				if (pop != null)
				{
					// First make sure we haven't already done this one
					if (done.contains(pop.getThing()))
						throw new IllegalStateException("Tried to set '" + pop.getThing() + "' twice; check @OldName annotations");
					else
						done.add(pop.getThing());
					
					Object value = property.getValue();
					value = this.convert(value, pop.getType());
					
					pop.populate(obj, value);
				}
			}
			
			return obj;
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/**
	 * Converts the value into an object suitable for the type (hopefully).
	 */
	Object convert(Object value, Class<?> type)
	{
		if (type == value.getClass())
		{
			return value;
		}
		if (type == String.class)
		{
			return value.toString();
		}
		else if (value instanceof Number)
		{
			Number number = (Number)value;
			if (type == Byte.class || type == Byte.TYPE) return number.byteValue();
			else if (type == Short.class || type == Short.TYPE) return number.shortValue();
			else if (type == Integer.class || type == Integer.TYPE) return number.intValue();
			else if (type == Long.class || type == Long.TYPE) return number.longValue();
			else if (type == Float.class || type == Float.TYPE) return number.floatValue();
			else if (type == Double.class || type == Double.TYPE) return number.doubleValue();
		}

		throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}
	
	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity toEntity(Object obj)
	{
		try
		{
			Entity ent = this.initEntity(obj);

			for (Field f: this.writeables)
			{
				Object value = f.get(obj);
				if (f.isAnnotationPresent(Indexed.class))
					ent.setProperty(f.getName(), value);
				else
					ent.setUnindexedProperty(f.getName(), value);
			}

			return ent;
		}
		catch (IllegalArgumentException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
	
	/**
	 * <p>This hides all the messiness of trying to create an Entity from an object that:</p>
	 * <ul>
	 * <li>Might have a long id, might have a String name</li>
	 * <li>If it's a Long id, might be null and require autogeneration</li>
	 * <li>Might have a parent key</li>
	 * </ul>
	 * <p>Gross, isn't it?</p>
	 */
	Entity initEntity(Object obj)
	{
		try
		{
			Key parentKey = null;
			
			// First thing, get the parentKey (if appropriate)
			if (this.parentField != null)
			{
				parentKey = (Key)this.parentField.get(obj);
				if (parentKey == null)
					throw new IllegalStateException("Missing parent of " + obj);
			}
			
			if (this.idField != null)
			{
				Long id = (Long)this.idField.get(obj);	// possibly null
				if (id != null)
				{
					if (this.parentField != null)
						return new Entity(KeyFactory.createKey(parentKey, this.kind, id));
					else
						return new Entity(KeyFactory.createKey(this.kind, id));
				}
				else // id is null, must autogenerate
				{
					if (this.parentField != null)
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
				
				if (this.parentField != null)
				{
					return new Entity(this.kind, name, parentKey);
				}
				else
				{
					return new Entity(this.kind, name);
				}
			}
		}
		catch (IllegalAccessException ex) { throw new RuntimeException(ex); }	
	}
	
	/**
	 * Sets the relevant id and parent fields of the object to the values stored in the key.
	 * Object must be of the entityClass type for this metadata.
	 */
	public void setKey(Object obj, Key key)
	{
		if (obj.getClass() != this.entityClass)
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
					throw new IllegalStateException("Loaded Entity has id but " + this.entityClass.getName() + " has no Long (or long) @Id");
				
				this.idField.set(obj, key.getId());
			}
			
			Key parentKey = key.getParent();
			if (parentKey != null)
			{
				if (this.parentField == null)
					throw new IllegalStateException("Loaded Entity has parent but " + this.entityClass.getName() + " has no @Parent");
				
				this.parentField.set(obj, parentKey);
			}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 * @param obj must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if obj has a null id
	 */
	public Key getKey(Object obj)
	{
		if (obj.getClass() != this.entityClass)
			throw new IllegalArgumentException("Trying to use metadata for " + this.entityClass.getName() + " to get key of " + obj.getClass().getName());

		try
		{
			if (this.nameField != null)
			{
				String name = (String)this.nameField.get(obj);
				
				if (this.parentField != null)
				{
					Key parent = (Key)this.parentField.get(obj);
					return KeyFactory.createKey(parent, this.kind, name);
				}
				else	// name yes parent no
				{
					return KeyFactory.createKey(this.kind, name);
				}
			}
			else	// has id not name
			{
				Long id = (Long)this.idField.get(obj);
				if (id == null)
					throw new IllegalArgumentException("You cannot create a Key for an object with a null @Id. Object was " + obj);
				
				if (this.parentField != null)
				{
					Key parent = (Key)this.parentField.get(obj);
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
}
