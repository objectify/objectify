package com.googlecode.objectify;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.persistence.Id;
import javax.persistence.Transient;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.annotation.OldName;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Unindexed;


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

	/** We need to be able to populate fields and methods with @OldName */
	static abstract class Populator
	{
		/** Actually populate the thing (field or method) */
		abstract void populate(Object entity, Object value);

		/** Get the thing for hashing, string conversion, etc */
		abstract Object getThing();

		/** Get the type of the thing */
		abstract Class<?> getType();
		
		/** Get the "generictype", which can be a ParameterizedType */
		abstract Type getGenericType();
		
		/** If getType() is an array or Collection, returns the component type - otherwise null */
		Class<?> getComponentType()
		{
			if (this.getType().isArray())
			{
				return this.getType().getComponentType();
			}
			else if (Collection.class.isAssignableFrom(this.getType()))
			{
				Type aType = this.getGenericType();
				while (aType instanceof Class<?>)
					aType = ((Class<?>)aType).getGenericSuperclass();
				
				if (aType instanceof ParameterizedType)
				{
					Type actualTypeArgument = ((ParameterizedType)aType).getActualTypeArguments()[0];
					if (actualTypeArgument instanceof Class<?>)
						return (Class<?>)actualTypeArgument;
					else if (actualTypeArgument instanceof ParameterizedType)
						return (Class<?>)((ParameterizedType)actualTypeArgument).getRawType();
					else
						return null;
				}
				else
				{
					return null;
				}
			}
			else	// not array or collection
			{
				return null;
			}
		}
	}

	/** Works with fields */
	static class FieldPopulator extends Populator
	{
		Field field;
		public FieldPopulator(Field field) { this.field = field; }
		public Object getThing() { return this.field; }
		public Class<?> getType() { return this.field.getType(); }
		public Type getGenericType() { return this.field.getGenericType(); }
		public void populate(Object entity, Object value)
		{
			try { this.field.set(entity, value); }
			catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
		}
	}

	/** Works with methods */
	static class MethodPopulator extends Populator
	{
		Method method;
		public MethodPopulator(Method method) { this.method = method; }
		public Object getThing() { return this.method; }
		public Class<?> getType() { return this.method.getParameterTypes()[0]; }
		public Type getGenericType() { return this.method.getGenericParameterTypes()[0]; }
		public void populate(Object entity, Object value)
		{
			try { this.method.invoke(entity, value); }
			catch (IllegalAccessException ex) { throw new RuntimeException(ex); }
			catch (InvocationTargetException ex) { throw new RuntimeException(ex); }
		}
	}
	
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

	/** The fields we persist, not including the @Id or @Parent fields */
	protected Set<Field> writeables = new HashSet<Field>();

	/** The things that we read, keyed by name (including @OldName fields and methods).  A superset of writeables. */
	protected Map<String, Populator> readables = new HashMap<String, Populator>();

	/** */
	public EntityMetadata(ObjectifyFactory fact, Class<T> clazz)
	{
		this.factory = fact;
		this.entityClass = clazz;
		this.kind = this.factory.getKind(clazz);

		// Recursively walk up the inheritance chain looking for fields
		this.visit(clazz);

		// There must be some field marked with @Id
		if ((this.idField == null) && (this.nameField == null))
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + this.entityClass.getName());
	}
	
	/** @return the kind associated with this metadata */
	public String getKind()
	{
		return this.kind;
	}

	/**
	 * Recursive function adds any appropriate fields to our internal data
	 * structures for persisting and retrieving later.
	 */
	private void visit(Class<?> clazz)
	{
		if ((clazz == null) || (clazz == Object.class))
			return;

		// Check all the fields
		for (Field field: clazz.getDeclaredFields())
		{
			if (field.isAnnotationPresent(Transient.class) || ((field.getModifiers() & BAD_MODIFIERS) != 0))
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

				if (field.getType() != Key.class && field.getType() != OKey.class)
					throw new IllegalStateException("Only fields of type OKey<?> or Key are allowed as @Parent. Illegal parent '" + field + "' in " + clazz.getName());

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
	public T toObject(Entity ent)
	{
		try
		{
			T obj = this.entityClass.newInstance();

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
					value = this.convertFromDatastore(value, pop.getType(), pop.getComponentType());

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
	 * For loading data out of the datastore.  Note that collections are
	 * quite complicated since they are always written as List<?>.
	 * 
	 * @param value is the property value that came out of the datastore Entity
	 * @param type is the type of the field or method param we are populating
	 * @param componentType is the type of a component of 'type' when 'type' is
	 *  an array or collection.  null if 'type' is not an array or collection.
	 */
	@SuppressWarnings("unchecked")
	Object convertFromDatastore(Object value, Class<?> type, Class<?> componentType)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof Collection<?>)
		{
			Collection<?> collValue = (Collection<?>)value;
			
			if (type.isArray())
			{
				// The objects in the Collection are assumed to be of correct type for the array
				Object array = Array.newInstance(componentType, collValue.size());
				
				int index = 0;
				for (Object componentValue: collValue)
				{
					componentValue = this.convertFromDatastore(componentValue, componentType, null);
					
					//System.out.println("componentType is " + componentType + ", componentValue class is " + componentValue.getClass());
					Array.set(array, index++, componentValue);
				}
				
				return array;
			}
			else if (Collection.class.isAssignableFrom(type)) // Check for collection early!
			{
				// We're making some sort of collection.  If it's a concrete class, just
				// instantiate it.  Otherwise it's an interface and we need to pick the
				// concrete class ourselves.
				Collection<Object> target = null;
				
				if (!type.isInterface())
				{
					try
					{
						target = (Collection<Object>)type.newInstance();
					}
					catch (InstantiationException e) { throw new RuntimeException(e); }
					catch (IllegalAccessException e) { throw new RuntimeException(e); }
				}
				else if (SortedSet.class.isAssignableFrom(type))
				{
					target = new TreeSet<Object>();
				}
				else if (Set.class.isAssignableFrom(type))
				{
					target = new HashSet<Object>();
				}
				else if (List.class.isAssignableFrom(type) || type.isAssignableFrom(ArrayList.class))
				{
					target = new ArrayList<Object>();
				}
				
				for (Object obj: collValue)
					target.add(this.convertFromDatastore(obj, componentType, null));
				
				return target;
			}
		}
		else if (type.isAssignableFrom(value.getClass()))
		{
			return value;
		}
		else if (type == String.class)
		{
			if (value instanceof Text)
				return ((Text)value).getValue();
			else
				return value.toString();
		}
		else if (Enum.class.isAssignableFrom(type))
		{
			// Anyone have any idea how to avoid this generics warning?
			return Enum.valueOf((Class<Enum>)type, value.toString());
		}
		else if ((value instanceof Boolean) && (type == Boolean.TYPE))
		{
			return value;
		}
		else if (value instanceof Number)
		{
			return this.coerceNumber((Number)value, type);
		}
		else if (value instanceof Key && OKey.class.isAssignableFrom(type))
		{
			return this.factory.rawKeyToOKey((Key)value);
		}

		throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}
	
	/**
	 * Coerces the value to be a number of the specified type; needed because
	 * all numbers come back from the datastore as Long and this screws up
	 * any type that expects something smaller.  Also does toString just for the
	 * hell of it.
	 */
	Object coerceNumber(Number value, Class<?> type)
	{
		if ((type == Byte.class) || (type == Byte.TYPE)) return value.byteValue();
		else if ((type == Short.class) || (type == Short.TYPE)) return value.shortValue();
		else if ((type == Integer.class) || (type == Integer.TYPE)) return value.intValue();
		else if ((type == Long.class) || (type == Long.TYPE)) return value.longValue();
		else if ((type == Float.class) || (type == Float.TYPE)) return value.floatValue();
		else if ((type == Double.class) || (type == Double.TYPE)) return value.doubleValue();
		else if (type == String.class) return value.toString();
		else throw new IllegalArgumentException("Don't know how to convert " + value.getClass() + " to " + type);
	}

	/**
	 * Converts the value into an object suitable for storing in the datastore.
	 */
	Object convertToDatastore(Object value)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof String)
		{
			// Check to see if it's too long and needs to be Text instead
			if (((String)value).length() > 500)
				return new Text((String)value);
		}
		else if (value instanceof Enum<?>)
		{
			return value.toString();
		}
		else if (value.getClass().isArray())
		{
			// The datastore cannot persist arrays, but it can persist ArrayList
			int length = Array.getLength(value);
			ArrayList<Object> list = new ArrayList<Object>(length);
			
			for (int i=0; i<length; i++)
				list.add(this.convertToDatastore(Array.get(value, i)));
			
			return list;
		}
		else if (value instanceof Collection<?>)
		{
			// All collections get turned into a List that preserves the order.  We must
			// also be sure to convert anything contained in the collection
			ArrayList<Object> list = new ArrayList<Object>(((Collection<?>)value).size());

			for (Object obj: (Collection<?>)value)
				list.add(this.convertToDatastore(obj));
			
			return list;
		}
		else if (value instanceof OKey<?>)
		{
			return this.factory.oKeyToRawKey((OKey<?>)value);
		}

		// Usually we just want to return the value
		return value;
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
				value = this.convertToDatastore(value);

				if (f.isAnnotationPresent(Unindexed.class))
					ent.setUnindexedProperty(f.getName(), value);
				else
					ent.setProperty(f.getName(), value);
					// TODO: Add warning if the field is indexed but we have converted it to Text (which is always unindexed).
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

				if (this.parentField.getType() == Key.class)
					this.parentField.set(obj, parentKey);
				else
					this.parentField.set(obj, this.factory.rawKeyToOKey(parentKey));
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
					Key parent = this.getRawKey(this.parentField, obj);
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
					Key parent = this.getRawKey(this.parentField, obj);
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
	
	/** @return the raw key even if the field is an OKey */
	private Key getRawKey(Field keyField, Object obj) throws IllegalAccessException
	{
		if (keyField.getType() == Key.class)
			return (Key)keyField.get(obj);
		else
			return this.factory.oKeyToRawKey((OKey<?>)keyField.get(obj));
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
