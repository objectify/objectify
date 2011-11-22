package com.googlecode.objectify.impl.save;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.IgnoreSave;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.condition.If;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Most savers are related to a particular type of field.  This provides
 * a convenient base class.</p>
 */
abstract public class FieldSaver implements Saver
{
	Field field;

	/** 
	 * If this is non-null, it means we have a class-provided default value that should override the current save mode.
	 * However, local @Index/@Unindex conditions get the final say. 
	 */
	Boolean defaultIndexed;

	/** These are authoritative */
	If<?, ?>[] indexConditions;
	If<?, ?>[] unindexConditions;
	If<?, ?>[] ignoreSaveConditions;
	
	/**
	 * @param examinedClass is the class which is being registered (or embedded).  It posesses the field,
	 * but it is not necessarily the declaring class (which could be a base class).
	 * @param ignoreClassIndexing if true will prevent the declaring class of this field from having an effect on indexing via its @Indexed/@Unindexed
	 * @param collectionize is whether or not the elements of this field should be stored in a collection;
	 * this is used for embedded collection class fields. 
	 */
	public FieldSaver(Class<?> examinedClass, Field field, boolean ignoreClassIndexing, boolean collectionize)
	{
		this.field = field;
		
		// This might be null if there is no explicit default
		if (!ignoreClassIndexing)
			this.defaultIndexed = TypeUtils.isClassIndexed(field.getDeclaringClass());

		// Check @Index and @Unindex conditions
		Index indexedAnn = field.getAnnotation(Index.class);
		Unindex unindexedAnn = field.getAnnotation(Unindex.class);

		if (indexedAnn != null && unindexedAnn != null)
			throw new IllegalStateException("Cannot have @Index and @Unindex on the same field: " + field);
		
		if (indexedAnn != null)
			this.indexConditions = this.generateIfConditions(indexedAnn.value(), examinedClass);
		
		if (unindexedAnn != null)
			this.unindexConditions = this.generateIfConditions(unindexedAnn.value(), examinedClass);
		
		// Now watch out for @IgnoreSave conditions
		IgnoreSave notSaved = field.getAnnotation(IgnoreSave.class);
		if (notSaved != null)
		{
			if (collectionize && (notSaved.value().length != 1 || notSaved.value()[0] != Always.class))
				throw new IllegalStateException("You cannot use @IgnoreSave with a condition within @Embed collections; check the field " + this.field);
			
			this.ignoreSaveConditions = this.generateIfConditions(notSaved.value(), examinedClass);
		}
	}
	
	/** */
	private If<?, ?>[] generateIfConditions(Class<? extends If<?, ?>>[] ifClasses, Class<?> examinedClass)
	{
		If<?, ?>[] result = new If<?, ?>[ifClasses.length];
		
		for (int i=0; i<ifClasses.length; i++)
		{
			Class<? extends If<?, ?>> ifClass = ifClasses[i];
			result[i] = this.createIf(ifClass, examinedClass);

			// Sanity check the generic If class types to ensure that they matches the actual types of the field & entity.
			List<Class<?>> typeArguments = TypeUtils.getTypeArguments(If.class, ifClass);
			
			if (!TypeUtils.isAssignableFrom(typeArguments.get(0), field.getType()))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because you cannot assign " + field.getType().getName() + " to " + typeArguments.get(0).getName());
			
			if (!TypeUtils.isAssignableFrom(typeArguments.get(1), examinedClass))
				throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field
						+ " because the containing class " + examinedClass.getName() + " is not compatible with " + typeArguments.get(1).getName());
		}
		
		return result;
	}
	
	/** */
	private If<?, ?> createIf(Class<? extends If<?, ?>> ifClass, Class<?> examinedClass)
	{
		try
		{
			Constructor<? extends If<?, ?>> ctor = TypeUtils.getConstructor(ifClass, Class.class, Field.class);
			return TypeUtils.newInstance(ctor, examinedClass, this.field);
		}
		catch (IllegalStateException ex)
		{
			try
			{
				Constructor<? extends If<?, ?>> ctor = TypeUtils.getNoArgConstructor(ifClass);
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
	final public void save(Object pojo, Entity entity, Path path, boolean index)
	{
		// First thing, if we have an explicit class-level default, use it
		if (this.defaultIndexed != null)
			index = this.defaultIndexed;
		
		Object value = TypeUtils.field_get(this.field, pojo);
		
		if (this.ignoreSaveConditions != null)
		{
			for (int i=0; i<this.ignoreSaveConditions.length; i++)
				if (((If<Object, Object>)this.ignoreSaveConditions[i]).matches(value, pojo))
					return;
		}
		
		if (this.indexConditions != null && !index)
		{
			for (int i=0; i<this.indexConditions.length; i++)
				if (((If<Object, Object>)this.indexConditions[i]).matches(value, pojo))
					index = true;
		}
		
		if (this.unindexConditions != null && index)
		{
			for (int i=0; i<this.unindexConditions.length; i++)
				if (((If<Object, Object>)this.unindexConditions[i]).matches(value, pojo))
					index = false;
		}
		
		this.saveValue(value, entity, path.extend(field.getName()), index);
	}
	
	/**
	 * Actually save the value in the entity.  This is the real value, already obtained
	 * from the POJO and checked against the @IgnoreSave mechanism..
	 * @param path TODO
	 */
	abstract protected void saveValue(Object value, Entity entity, Path path, boolean index);

	/** 
	 * Sets property on the entity correctly for the values of this.path and this.indexed.
	 * @param path TODO
	 */
	protected void setEntityProperty(Entity entity, Object value, Path path, boolean index)
	{
		if (index)
			entity.setProperty(path.toPathString(), value);
		else
			entity.setUnindexedProperty(path.toPathString(), value);
	}

	/**
	 * @return true if the field has an explicit indexing instruction (this will override any index instruction on the class)
	 */
	protected boolean hasExplicitIndexingInstruction()
	{
		return this.field.isAnnotationPresent(Index.class) || this.field.isAnnotationPresent(Unindex.class);
	}
}
