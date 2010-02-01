package com.googlecode.objectify;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.impl.ClassSerializer;
import com.googlecode.objectify.impl.ObjectHolder;
import com.googlecode.objectify.impl.TypeUtils;


/**
 * Everything you need to know about mapping between Datastore Entity objects
 * and typed entity objects.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityMetadata<T>
{
	/** We do not persist fields with any of these modifiers */
	static final int BAD_MODIFIERS = Modifier.FINAL | Modifier.STATIC | Modifier.TRANSIENT;

	/** Needed for key translation */
	protected ObjectifyFactory factory;

	/** */
	protected Class<T> entityClass;
	public Class<T> getEntityClass() { return this.entityClass; }

	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	protected String kind;

	/** We treat the @Id key field specially - it will be either Long id or String name */
	protected Field idField;
	protected Field nameField;

	/** If the entity has a @Parent field, treat it specially */
	protected Field parentField;

	private final ClassSerializer classinfo;


	/**
	 * Inspects and stores the metadata for a particular entity class.
	 * @param clazz must be a properly-annotated Objectify entity class.
	 */
	public EntityMetadata(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		this.entityClass = clazz;
		this.kind = this.factory.getKind(clazz);
		this.classinfo = new ClassSerializer(fact, clazz);

		// Recursively walk up the inheritance chain looking for fields
		this.visit(clazz, "", classinfo, false, false);
		classinfo.verify();

		// There must be some field marked with @Id
		if ((this.idField == null) && (this.nameField == null))
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + this.entityClass.getName());

		TypeUtils.checkForNoArgConstructor(clazz);
	}

	/** @return the datastore kind associated with this metadata */
	public String getKind()
	{
		return this.kind;
	}

	/**
	 * Recursive function adds any appropriate fields to our internal data
	 * structures for persisting and retrieving later.  Walks not only the
	 * parent hierarchy, but also any embedded classes (and their parents).
	 * This will walk through a potentially large tree.
	 * 
	 * @param clazz is the class to inspect.  All parent and embedded classes will also be visited.
	 * @param propPrefix is the prefix for embedded class fields, starting at "" for root classes and going to "fieldname." and "fieldname.another."
	 * @param forceUnindexed will cause all further properties to be treated as @Unindexed
	 * @param embeddedClass is true if we are visiting an embedded class
	 */
	private void visit(Class<?> clazz, String propPrefix, ClassSerializer classinfo, boolean forceUnindexed, boolean embeddedClass)
	{
		if ((clazz == null) || (clazz == Object.class))
			return;

		this.visit(clazz.getSuperclass(), propPrefix, classinfo, forceUnindexed, embeddedClass);

		// Check all the fields
		for (Field field: clazz.getDeclaredFields())
		{
			if (field.isAnnotationPresent(Transient.class) || ((field.getModifiers() & BAD_MODIFIERS) != 0))
				continue;

			field.setAccessible(true);

			if (field.isAnnotationPresent(Id.class))
			{
				if (embeddedClass)
					throw new IllegalStateException("@Id not supported on embedded class " + clazz);
				
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
				if (embeddedClass)
					throw new IllegalStateException("@Parent not supported on embedded class " + clazz);
				
				if (this.parentField != null)
					throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + this.entityClass.getName());

				if (field.getType() != com.google.appengine.api.datastore.Key.class && field.getType() != Key.class)
					throw new IllegalStateException("Only fields of type Key<?> or Key are allowed as @Parent. Illegal parent '" + field + "' in " + clazz.getName());

				this.parentField = field;
			}
			else
			{
				boolean hasUnindexed = field.isAnnotationPresent(Unindexed.class);
				boolean indexed = !hasUnindexed && !forceUnindexed;
				boolean embedded = field.isAnnotationPresent(javax.persistence.Embedded.class);
				String name = propPrefix + field.getName();

				boolean isArray = field.getType().isArray();
				boolean isCollection = Collection.class.isAssignableFrom(field.getType());

				if (embedded)
				{
					ClassSerializer subinfo;
					Class<?> childType;
					if (isArray)
					{
						childType = field.getType().getComponentType();
						if (childType.isPrimitive())
						{
							throw new IllegalStateException("Can't use @Embedded on '" + field.getName() + "' in " +
									clazz.getName() + " because it is a primitive array. It will work fine without @Embedded.");
						}
						
						subinfo = classinfo.addEmbeddedArrayField(field, name);
					}
					else
					{
						childType = field.getType();
						subinfo = classinfo.addEmbeddedField(field, name, indexed);
					}

					visit(childType, name + ".", subinfo, forceUnindexed || hasUnindexed, true);
				}
				else	// not embedded
				{
					classinfo.addField(field, name, isArray || isCollection, indexed);
				}
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

				classinfo.addMethod(method, oldName.value());
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
		try
		{
			T obj = this.entityClass.newInstance();

			// This will set the id and parent fields as appropriate.
			this.setKey(obj, ent.getKey());

			classinfo.loadIntoObject(ent, new ObjectHolder(obj));

			return obj;
		}
		catch (InstantiationException e) { throw new RuntimeException(e); }
		catch (IllegalAccessException e) { throw new RuntimeException(e); }
	}


	/**
	 * Converts an object to a datastore Entity with the appropriate Key type.
	 */
	public Entity toEntity(T obj)
	{
		Entity ent = this.initEntity(obj);

		classinfo.toEntity(ent, obj);
		return ent;
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
					this.parentField.set(obj, this.factory.rawKeyToOKey(parentKey));
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
			return this.factory.oKeyToRawKey((Key<?>)keyField.get(obj));
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
