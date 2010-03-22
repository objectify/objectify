package com.googlecode.objectify.impl.save;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.annotation.NotSaved;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Most savers are related to a particular type of field.  This provides
 * a convenient base class.</p>
 */
abstract public class FieldSaver implements Saver
{
	String path;
	Field field;
	If<?>[] indexConditions;
	If<?>[] unindexConditions;
	If<?>[] notSavedConditions;
	
	/**
	 * @param examinedClass is the class which is being registered (or embedded).  It posesses the field,
	 * but it is not necessarily the declaring class (which could be a base class).
	 * @param collectionize is whether or not the elements of this field should be stored in a collection;
	 * this is used for embedded collection class fields. 
	 */
	public FieldSaver(String pathPrefix, Class<?> examinedClass, Field field, boolean collectionize)
	{
		this.field = field;
		this.path = TypeUtils.extendPropertyPath(pathPrefix, field.getName());

		// Check @Indexed and @Unindexed conditions
		Indexed indexedAnn = field.getAnnotation(Indexed.class);
		Unindexed unindexedAnn = field.getAnnotation(Unindexed.class);

		if (indexedAnn != null && unindexedAnn != null)
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same field: " + field);
		
		if (indexedAnn != null)
			this.indexConditions = this.generateIfConditions(indexedAnn.value(), examinedClass);
		
		if (unindexedAnn != null)
			this.unindexConditions = this.generateIfConditions(unindexedAnn.value(), examinedClass);
		
		// Now watch out for @NotSaved conditions
		NotSaved notSaved = field.getAnnotation(NotSaved.class);
		if (notSaved != null)
		{
			if (collectionize && (notSaved.value().length != 1 || notSaved.value()[0] != Always.class))
				throw new IllegalStateException("You cannot use @NotSaved with a condition within @Embedded collections; check the field " + this.field);
			
			this.notSavedConditions = this.generateIfConditions(notSaved.value(), examinedClass);
		}
	}
	
	/** */
	private If<?>[] generateIfConditions(Class<? extends If<?>>[] ifClasses, Class<?> examinedClass)
	{
		If<?>[] result = new If<?>[ifClasses.length];
		
		for (int i=0; i<ifClasses.length; i++)
		{
			Class<? extends If<?>> ifClass = ifClasses[i];
			result[i] = this.createIf(ifClass, examinedClass);

			// Sanity check the generic If class type to ensure that it matches the actual type of the field.
			Class<?> typeArgument = TypeUtils.getTypeArguments(If.class, ifClass).get(0);
			if (!TypeUtils.isAssignableFrom(typeArgument, field.getType()))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because you cannot assign " + field.getType().getName() + " to " + typeArgument.getName());
		}
		
		return result;
	}
	
	/** */
	private If<?> createIf(Class<? extends If<?>> ifClass, Class<?> examinedClass)
	{
		try
		{
			Constructor<? extends If<?>> ctor = TypeUtils.getConstructor(ifClass, Class.class, Field.class);
			return TypeUtils.newInstance(ctor, examinedClass, this.field);
		}
		catch (IllegalStateException ex)
		{
			try
			{
				Constructor<? extends If<?>> ctor = TypeUtils.getNoArgConstructor(ifClass);
				return TypeUtils.newInstance(ctor);
			}
			catch (IllegalStateException ex2)
			{
				throw new IllegalStateException("The If<?> class " + ifClass.getName() + " must have a no-arg constructor or a constructor that takes one argument of type Field.");
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	@SuppressWarnings("unchecked")
	final public void save(Object pojo, Entity entity, boolean index)
	{
		Object value = TypeUtils.field_get(this.field, pojo);
		
		if (this.notSavedConditions != null)
		{
			for (int i=0; i<this.notSavedConditions.length; i++)
				if (((If<Object>)this.notSavedConditions[i]).matches(value))
					return;
		}
		
		if (this.indexConditions != null && !index)
		{
			for (int i=0; i<this.indexConditions.length; i++)
				if (((If<Object>)this.indexConditions[i]).matches(value))
					index = true;
		}
		
		if (this.unindexConditions != null && index)
		{
			for (int i=0; i<this.unindexConditions.length; i++)
				if (((If<Object>)this.unindexConditions[i]).matches(value))
					index = false;
		}
		
		this.saveValue(value, entity, index);
	}
	
	/**
	 * Actually save the value in the entity.  This is the real value, already obtained
	 * from the POJO and checked against the @Unsaved mechanism..
	 */
	abstract protected void saveValue(Object value, Entity entity, boolean index);

	/** 
	 * Sets property on the entity correctly for the values of this.path and this.indexed.
	 */
	protected void setEntityProperty(Entity entity, Object value, boolean index)
	{
		if (index)
			entity.setProperty(this.path, value);
		else
			entity.setUnindexedProperty(this.path, value);
	}
}
