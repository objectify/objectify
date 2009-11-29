package com.googlecode.objectify;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;


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
	
	/** We treat the @Id key field specially */
	private Field keyField;
	
	/** The fields we persist, not including the @Id key field */
	private Set<Field> writeables;
	
	/** The fields that we read, keyed by name - including @OldName fields.  A superset of writeables. */
	private Map<String, Field> readables;
	
	/** */
	public EntityMetadata(Class<?> clazz)
	{
		this.entityClass = clazz;
		
		// Recursively walk up the inheritance chain looking for fields
		this.visit(clazz);
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
			
			if (field.isAnnotationPresent(Id.class))
			{
				if (field.getType() != Key.class)
					throw new IllegalStateException("Only fields of type Key are allowed as @Id. Invalid type found in " + clazz.getName());
				else if (this.keyField != null)
					throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + this.entityClass.getName());
				else
					this.keyField = field;
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
			this.keyField.set(obj, ent.getKey());

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
						throw new IllegalStateException("Tried to populate the field " + f + " twice; data exists for @OldName and the current name");
					else
						done.add(f);
					
					f.set(obj, property.getValue());
				}
			}
			
			return obj;
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}
	
	/**
	 * Converts an object to a datastore Entity.  If the key is null, it will be created.
	 */
	public Entity toEntity(Object obj)
	{
		try
		{
			Key key = (Key)this.keyField.get(obj);
			
			Entity ent = (key == null)
				? new Entity(ObjectifyFactory.getKind(this.entityClass))
				: new Entity(key);

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
}
