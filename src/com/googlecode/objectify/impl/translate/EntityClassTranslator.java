package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.EntitySubclass;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.KeyMetadata;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslatableProperty;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.DatastoreUtils;


/**
 * <p>Translator which can maps the root of an entity.  There is no factory associated with this; you just
 * instantiate one as necessary.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EntityClassTranslator<T> extends ClassTranslator<T> implements KeyMetadata<T>
{
	/** */
	private static final Logger log = Logger.getLogger(EntityClassTranslator.class.getName());

	/** The @Id field on the pojo - it will be Long, long, or String */
	TranslatableProperty<Object> idMeta;

	/** The @Parent field on the pojo, or null if there is no parent */
	TranslatableProperty<Object> parentMeta;
	
	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	String kind;
	
	/**
	 */
	@SuppressWarnings("unchecked")
	public EntityClassTranslator(Type type, CreateContext ctx) {
		super((Class<T>)GenericTypeReflector.erase(type), Path.root(), ctx);
		
		// We should never have gotten this far in the registration process
		assert clazz.getAnnotation(Entity.class) != null || clazz.getAnnotation(EntitySubclass.class) != null;

		// There must be some field marked with @Id
		if (this.idMeta == null)
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + clazz.getName());

		this.kind = Key.getKind(clazz);
	}
	
	/**
	 * Look for the id and parent properties
	 */
	@Override
	protected void foundTranslatableProperty(TranslatableProperty<Object> tprop) {
		
		Property prop = tprop.getProperty();
		
		if (prop.getAnnotation(Id.class) != null) {
			if (this.idMeta != null)
				throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + clazz.getName());

			if ((prop.getType() != Long.class) && (prop.getType() != long.class) && (prop.getType() != String.class))
				throw new IllegalStateException("@Id field '" + prop.getName() + "' in " + clazz.getName() + " must be of type Long, long, or String");
			
			this.idMeta = tprop;
		}
		else if (prop.getAnnotation(Parent.class) != null) {
			if (this.parentMeta != null)
				throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + clazz.getName());

			if (!isAllowedParentFieldType(prop.getType()))
				throw new IllegalStateException("@Parent fields must be Key<?>, datastore Key, Ref<?>, or a pojo entity type. Illegal parent: " + prop);

			this.parentMeta = tprop;
		}
	}

	/** @return true if the type is an allowed parent type */
	private boolean isAllowedParentFieldType(Type type) {
		
		Class<?> erased = GenericTypeReflector.erase(type);
		
		return com.google.appengine.api.datastore.Key.class.isAssignableFrom(erased)
				|| Key.class.isAssignableFrom(erased)
				|| Ref.class.isAssignableFrom(erased)
				|| erased.isAnnotationPresent(com.googlecode.objectify.annotation.Entity.class)
				|| erased.isAnnotationPresent(EntitySubclass.class);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#getKind()
	 */
	@Override
	public String getKind() {
		return kind;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#setKey(java.lang.Object, com.google.appengine.api.datastore.Key, com.googlecode.objectify.impl.translate.LoadContext)
	 */
	@Override
	public void setKey(T pojo, com.google.appengine.api.datastore.Key key, LoadContext ctx) {
		if (!clazz.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + clazz.getName() + " to set key of " + pojo.getClass().getName());

		idMeta.setValue(pojo, DatastoreUtils.getId(key), ctx);
		
		com.google.appengine.api.datastore.Key parentKey = key.getParent();
		if (parentKey != null) {
			if (this.parentMeta == null)
				throw new IllegalStateException("Loaded Entity has parent but " + clazz.getName() + " has no @Parent");
			
			parentMeta.executeLoad(Node.of(parentKey), pojo, ctx);
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#initEntity(java.lang.Object)
	 */
	@Override
	public com.google.appengine.api.datastore.Entity initEntity(T pojo)
	{
		Object id = getId(pojo);
		if (id == null)
			if (isIdNumeric()) {
				if (log.isLoggable(Level.FINEST))
					log.finest("Getting parent key from " + pojo);
				
				return new com.google.appengine.api.datastore.Entity(this.kind, getParentRaw(pojo));
			} else
				throw new IllegalStateException("Cannot save an entity with a null String @Id: " + pojo);
		else
			return new com.google.appengine.api.datastore.Entity(getRawKey(pojo));
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#getRawKey(java.lang.Object)
	 */
	@Override
	public com.google.appengine.api.datastore.Key getRawKey(T pojo) {

		if (log.isLoggable(Level.FINEST))
			log.finest("Getting key from " + pojo);
		
		if (!clazz.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + clazz.getName() + " to get key of " + pojo.getClass().getName());

		com.google.appengine.api.datastore.Key parent = getParentRaw(pojo);
		Object id = getId(pojo);
		
		if (id == null)
			throw new IllegalArgumentException("You cannot create a Key for an object with a null @Id. Object was " + pojo);
		
		return DatastoreUtils.createKey(parent, kind, id);
	}
		
	/**
	 * Get the contents of the @Parent field as a datastore key.
	 * @return null if there was no @Parent field, or the field is null.
	 */
	private com.google.appengine.api.datastore.Key getParentRaw(T pojo) {
		if (parentMeta == null)
			return null;
		
		return (com.google.appengine.api.datastore.Key)parentMeta.getValue(pojo);
	}

	/**
	 * Get whatever is in the @Id field of the pojo doing no type checking or conversion
	 * @return Long or String or null
	 */
	private Object getId(T pojo) {
		return idMeta.getProperty().get(pojo);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#getParentFieldName()
	 */
	@Override
	public String getParentFieldName() {
		return parentMeta == null ? null : parentMeta.getProperty().getName();
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#getIdFieldName()
	 */
	@Override
	public String getIdFieldName() {
		return idMeta.getProperty().getName();
	}
	
	/**
	 * @return true if the id field is numeric, false if it is String
	 */
	private boolean isIdNumeric() {
		return !(this.idMeta.getProperty().getType() == String.class);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#hasParentField()
	 */
	@Override
	public boolean hasParentField() {
		return this.parentMeta != null;
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#shouldLoadParent(java.util.Set)
	 */
	@Override
	public boolean shouldLoadParent(Set<String> enabledGroups) {
		if (this.parentMeta == null)
			return false;
		
		return parentMeta.getProperty().shouldLoad(enabledGroups);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#isIdGeneratable()
	 */
	@Override
	public boolean isIdGeneratable() {
		return this.idMeta.getProperty().getType() == Long.class;
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.KeyMetadata#setLongId(java.lang.Object, java.lang.Long)
	 */
	@Override
	public void setLongId(T pojo, Long id) {
		if (!clazz.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + clazz.getName() + " to set key of " + pojo.getClass().getName());

		this.idMeta.getProperty().set(pojo, id);
	}
}
