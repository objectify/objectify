package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;


/**
 * Manages mapping of key/parent fields on pojo entities.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyMetadata<T>
{
	/** */
	protected ObjectifyFactory fact;
	
	/** */
	protected Class<T> entityClass;

	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	protected String kind;
	
	/** The @Id field on the pojo - it will be Long, long, or String */
	protected Field idField;

	/** The @Parent field on the pojo, or null if there is no parent */
	protected Field parentField;
	
	/** For translating between pojos and entities */
	protected Transmog<T> transmog;
	
	/**
	 * Inspects and stores the metadata for a particular entity class.
	 * @param clazz must be a properly-annotated Objectify entity class.
	 */
	public KeyMetadata(ObjectifyFactory fact, Class<T> clazz)
	{
		this.fact = fact;
		this.entityClass = clazz;
		this.kind = Key.getKind(clazz);
		
		// Recursively walk up the inheritance chain looking for @Id and @Parent fields
		this.processKeyFields(clazz);

		// There must be some field marked with @Id
		if (this.idField == null)
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + clazz.getName());
	}

	/** @return the datastore kind associated with this metadata */
	public String getKind()
	{
		return this.kind;
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
				if (this.idField != null)
					throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + this.entityClass.getName());

				if ((field.getType() == Long.class) || (field.getType() == Long.TYPE) || (field.getType() == String.class))
					this.idField = field;
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
	 * <p>This hides all the messiness of trying to create an Entity from an object that:</p>
	 * <ul>
	 * <li>Might have a long id, might have a String name</li>
	 * <li>If it's a Long id, might be null and require autogeneration</li>
	 * <li>Might have a parent key</li>
	 * </ul>
	 * 
	 * @return an empty Entity object whose key has been set but no other properties.
	 */
	public Entity initEntity(T pojo)
	{
		Object id = getId(pojo);
		if (id == null)
			if (isIdNumeric())
				return new Entity(this.kind, getParentRaw(pojo));
			else
				throw new IllegalStateException("Cannot save an entity with a null String @Id: " + pojo);
		else
			return new Entity(getRawKey(pojo));
	}

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 * 
	 * @param obj must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if obj has a null id
	 */
	public com.google.appengine.api.datastore.Key getRawKey(T pojo) {
		
		if (!this.entityClass.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + this.entityClass.getName() + " to get key of " + pojo.getClass().getName());

		com.google.appengine.api.datastore.Key parent = getParentRaw(pojo);
		Object id = getId(pojo);
		
		if (id == null)
			throw new IllegalArgumentException("You cannot create a Key for an object with a null @Id. Object was " + pojo);
		
		if (id instanceof String)
			return KeyFactory.createKey(parent, this.kind, (String)id);
		else
			return KeyFactory.createKey(parent, this.kind, (Long)id);
	}
		
	/**
	 * Get whatever is in the @Parent field of the pojo doing no type checking or conversion
	 * @return null if there was no @Parent field, or the field is null.
	 */
	public Object getParent(T pojo) {
		return parentField == null ? null : TypeUtils.field_get(parentField, pojo);
	}
	
	/**
	 * Get the contents of the @Parent field as a datastore key.
	 * @return null if there was no @Parent field, or the field is null.
	 */
	public com.google.appengine.api.datastore.Key getParentRaw(T pojo) {
		Object parent = getParent(pojo);
		return parent == null ? null : fact.getRawKey(parent);
	}

	/**
	 * Get whatever is in the @Id field of the pojo doing no type checking or conversion
	 * @return Long or String or null
	 */
	public Object getId(T pojo) {
		return TypeUtils.field_get(idField, pojo);
	}
	
	/** @return the name of the parent field, or null if there wasn't one */
	public String getParentFieldName() {
		return parentField == null ? null : parentField.getName();
	}

	/** @return the name of the id field */
	public String getIdFieldName() {
		return idField.getName();
	}
	
	/**
	 * @return true if the id field is numeric, false if it is String
	 */
	public boolean isIdNumeric() {
		return !(this.idField.getType() == String.class);
	}
	
	/**
	 * @return true if the entity has a parent field
	 */
	public boolean hasParentField() {
		return this.parentField != null;
	}
	
	/**
	 * @return true if the parent should be loaded given the enabled fetch groups
	 */
	public boolean shouldLoadParent(Set<String> enabledGroups)
	{
		if (this.parentField == null)
			return false;
		
		Load load = this.parentField.getAnnotation(Load.class);
		if (load == null)
			return false;
		
		if (load.value().length == 0)
			return true;
		
		for (String group: load.value())
			if (enabledGroups.contains(group))
				return true;
		
		return false;
	}
}
