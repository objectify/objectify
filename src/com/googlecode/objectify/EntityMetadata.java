package com.googlecode.objectify;

import java.lang.reflect.Field;
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
	
	/** The fields that we read, keyed by name - including @OldName fields.  A superset of writeables. */
	private Map<String, Field> readables = new HashMap<String, Field>();
	
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
	 * 
	 * TODO:  look for @OldName methods
	 */
	private void visit(Class<?> clazz)
	{
		if (clazz == null || clazz == Object.class)
			return;
		
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
				if (this.idField != null || this.nameField != null)
					throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + this.entityClass.getName());
				
				if (field.getType() != Key.class)
					throw new IllegalStateException("Only fields of type Key are allowed as @Parent. Illegal parent '" + field + "' in " + clazz.getName());
			}
			else
			{
				this.writeables.add(field);
				this.putReadable(field.getName(), field);
				
				OldName old = field.getAnnotation(OldName.class);
				if (old != null)
					this.putReadable(old.value(), field);
			}
		}
	}
	
	/**
	 * Adds the key/value pair to this.readables, throwing an exception if there
	 * is a duplicate key.
	 */
	private void putReadable(String name, Field f)
	{
		if (this.readables.put(name, f) != null)
			throw new IllegalStateException(
					"Field name '" + name + "' is duplicated in hierarchy of " + 
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

			// Keep track of which fields have been done so we don't repeat any;
			// this could happen if an Entity has data for both @OldName and the current name. 
			Set<Field> done = new HashSet<Field>();
			
			for (Map.Entry<String, Object> property: ent.getProperties().entrySet())
			{
				Field f = this.readables.get(property.getKey());
				if (f != null)
				{
					// First make sure we haven't already done this one
					if (done.contains(f))
						throw new IllegalStateException("Tried to populate the field '" + f + "' twice; check @OldName annotations");
					else
						done.add(f);
					
					Object value = property.getValue();
					
					// One quick default conversion - if the field we are setting is a String
					// but the property value is not a string, toString() it.  All other conversions
					// should be explicitly handled by @OldName on a setter method.
					if (f.getType().equals(String.class) && !(value instanceof String))
						value = value.toString();
					
					f.set(obj, value);
				}
			}
			
			return obj;
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
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
				Long id = this.idField.getLong(obj);	// possibly null
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
			throw new IllegalArgumentException("Using metadata for " + this.entityClass.getName() + " to set key of " + obj.getClass().getName());

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
				
				this.parentField.set(obj, parentKey.getParent());
			}
		}
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
}
