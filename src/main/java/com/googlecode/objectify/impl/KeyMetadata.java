package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.DatastoreUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Figures out what to do with key fields on POJO entities.
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class KeyMetadata<P>
{
	/** */
	private static final Logger log = Logger.getLogger(KeyMetadata.class.getName());

	/** The @Id field on the pojo - it will be Long, long, or String */
	private PropertyPopulator<Object, Object> idMeta;

	/** The @Parent field on the pojo, or null if there is no parent */
	private PropertyPopulator<Object, Object> parentMeta;

	/** */
	private Class<P> clazz;

	/** The kind that is associated with the class, ala ObjectifyFactory.getKind(Class<?>) */
	private String kind;

	/** */
	public KeyMetadata(Class<P> clazz, CreateContext ctx, Path path) {
		this.clazz = clazz;

		findKeyFields(clazz, ctx, path);

		// There must be some field marked with @Id
		if (this.idMeta == null)
			throw new IllegalStateException("There must be an @Id field (String, Long, or long) for " + clazz.getName());

		this.kind = Key.getKind(clazz);
	}

	/**
	 * Recursively go through the class hierarchy looking for the idMeta and parentMeta fields.
	 * @throws IllegalStateException if @Id or @Parent shows up twice.
	 */
	private void findKeyFields(Class<?> inspect, CreateContext ctx, Path path) {
		if (inspect == Object.class)
			return;

		findKeyFields(inspect.getSuperclass(), ctx, path);

		for (Field field: inspect.getDeclaredFields()) {
			if (field.getAnnotation(Id.class) != null) {
				if (this.idMeta != null)
					throw new IllegalStateException("Multiple @Id fields in the class hierarchy of " + clazz.getName());

				if ((field.getType() != Long.class) && (field.getType() != long.class) && (field.getType() != String.class))
					throw new IllegalStateException("@Id field '" + field.getName() + "' in " + inspect.getName() + " must be of type Long, long, or String");

				Property prop = new FieldProperty(ctx.getFactory(), clazz, field);
				Translator<Object, Object> translator = ctx.getTranslator(prop.getType(), prop.getAnnotations(), ctx, path.extend(prop.getName()));

				this.idMeta = new PropertyPopulator<>(prop, translator);

			} else if (field.getAnnotation(Parent.class) != null) {
				if (this.parentMeta != null)
					throw new IllegalStateException("Multiple @Parent fields in the class hierarchy of " + clazz.getName());

				if (!isAllowedParentFieldType(field.getType()))
					throw new IllegalStateException("@Parent fields must be Ref<?>, Key<?>, or datastore Key. Illegal parent: " + field);

				Property prop = new FieldProperty(ctx.getFactory(), clazz, field);
				Translator<Object, Object> translator = ctx.getTranslator(prop.getType(), prop.getAnnotations(), ctx, path.extend(prop.getName()));

				this.parentMeta = new PropertyPopulator<>(prop, translator);
			}
		}
	}

	/** @return true if the type is an allowed parent type */
	private boolean isAllowedParentFieldType(Type type) {

		Class<?> erased = GenericTypeReflector.erase(type);

		return com.google.appengine.api.datastore.Key.class.isAssignableFrom(erased)
				|| Key.class.isAssignableFrom(erased)
				|| Ref.class.isAssignableFrom(erased);
	}

	/**
	 * Sets the key onto the POJO id/parent fields
	 */
	public void setKey(P pojo, com.google.appengine.api.datastore.Key key, LoadContext ctx, Path containerPath) {
		if (!clazz.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + clazz.getName() + " to set key of " + pojo.getClass().getName());

		idMeta.setValue(pojo, DatastoreUtils.getId(key), ctx, containerPath);

		com.google.appengine.api.datastore.Key parentKey = key.getParent();
		if (parentKey != null) {
			if (this.parentMeta == null)
				throw new IllegalStateException("Loaded Entity has parent but " + clazz.getName() + " has no @Parent");

			parentMeta.setValue(pojo, parentKey, ctx, containerPath);
		}
	}

	/** @return the datastore kind associated with this metadata */
	public String getKind() {
		return kind;
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
	public Entity initEntity(P pojo) {
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

	/**
	 * Gets a key composed of the relevant id and parent fields in the object.
	 *
	 * @param pojo must be of the entityClass type for this metadata.
	 * @throws IllegalArgumentException if pojo has a null id
	 */
	public com.google.appengine.api.datastore.Key getRawKey(P pojo) {
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

	/** @return the name of the parent field, or null if there wasn't one */
	public String getParentFieldName() {
		return parentMeta == null ? null : parentMeta.getProperty().getName();
	}

	/** @return the name of the id field */
	public String getIdFieldName() {
		return idMeta.getProperty().getName();
	}

	/** @return the java type of the id field; it will be either Long.class, Long.TYPE, or String.class */
	public Class<?> getIdFieldType() {
		// The id must be Long, long, or String, therefore the type is always a Class
		return (Class<?>)idMeta.getProperty().getType();
	}

	/**
	 * @return true if the id field is numeric, false if it is String
	 */
	private boolean isIdNumeric() {
		return !(this.idMeta.getProperty().getType() == String.class);
	}

	/**
	 * @return true if the entity has a parent field
	 */
	public boolean hasParentField() {
		return this.parentMeta != null;
	}

	/**
	 * @return true if the parent should be loaded given the enabled fetch groups
	 */
	public boolean shouldLoadParent(Set<Class<?>> enabledGroups) {
		if (this.parentMeta == null)
			return false;

		return parentMeta.getProperty().shouldLoad(enabledGroups);
	}

	/**
	 * @return true if the id field is uppercase-Long, which can be genearted.
	 */
	public boolean isIdGeneratable() {
		return this.idMeta.getProperty().getType() == Long.class;
	}

	/**
	 * Sets the numeric id field
	 */
	public void setLongId(P pojo, Long id) {
		if (!clazz.isAssignableFrom(pojo.getClass()))
			throw new IllegalArgumentException("Trying to use metadata for " + clazz.getName() + " to set key of " + pojo.getClass().getName());

		this.idMeta.getProperty().set(pojo, id);
	}

	/**
	 * Get the contents of the @Parent field as a datastore key.
	 * @return null if there was no @Parent field, or the field is null.
	 */
	private com.google.appengine.api.datastore.Key getParentRaw(P pojo) {
		if (parentMeta == null)
			return null;

		// TODO: The null-ofy SaveContext is a little weird here. There must be a better way.
		return (com.google.appengine.api.datastore.Key)parentMeta.getValue(pojo, new SaveContext(null), Path.root());
	}

	/**
	 * Get whatever is in the @Id field of the pojo doing no type checking or conversion
	 * @return Long or String or null
	 */
	private Object getId(P pojo) {
		return idMeta.getProperty().get(pojo);
	}

}
