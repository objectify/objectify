package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.LoadOnly;
import com.googlecode.objectify.annotation.Unindexed;
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
	boolean indexed;
	boolean forcedInherit;	// will any child classes be forced to inherit this indexed state
	If<?>[] unsavedConditions;
	
	/** */
	public FieldSaver(String pathPrefix, Field field, boolean inheritedIndexed)
	{
		if (field.isAnnotationPresent(Indexed.class) && field.isAnnotationPresent(Unindexed.class))
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same field: " + field);
		
		this.field = field;
		
		this.path = TypeUtils.extendPropertyPath(pathPrefix, field.getName());
		
		this.indexed = inheritedIndexed;
		if (field.isAnnotationPresent(Indexed.class))
		{
			this.indexed = true;
			this.forcedInherit = true;
		}
		else if (field.isAnnotationPresent(Unindexed.class))
		{
			this.indexed = false;
			this.forcedInherit = true;
		}
		
		// Now watch out for LoadOnly conditions
		LoadOnly lo = field.getAnnotation(LoadOnly.class);
		if (lo != null)
		{
			this.unsavedConditions = new If<?>[lo.value().length];
			
			for (int i=0; i<lo.value().length; i++)
			{
				Class<? extends If<?>> ifClass = lo.value()[i];
				this.unsavedConditions[i] = TypeUtils.newInstance(ifClass);

				// Sanity check the generic If class type to ensure that it matches the actual type of the field.
				Class<?> typeArgument = TypeUtils.getTypeArguments(If.class, ifClass).get(0);
				if (!typeArgument.isAssignableFrom(field.getType()))
					throw new IllegalStateException("Cannot use If class " + ifClass.getName() + " on " + field);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	@SuppressWarnings("unchecked")
	final public void save(Object pojo, Entity entity)
	{
		Object value = TypeUtils.field_get(this.field, pojo);
		
		if (this.unsavedConditions != null)
		{
			for (int i=0; i<this.unsavedConditions.length; i++)
				if (((If<Object>)this.unsavedConditions[i]).matches(value))
					return;
		}
		
		this.saveValue(value, entity);
	}
	
	/**
	 * Actually save the value in the entity.  This is the real value, already obtained
	 * from the POJO and checked against the loadonly mechanism..
	 */
	abstract protected void saveValue(Object value, Entity entity);

	/** 
	 * Sets property on the entity correctly for the values of this.path and this.indexed.
	 */
	protected void setEntityProperty(Entity entity, Object value)
	{
		if (this.indexed)
			entity.setProperty(this.path, value);
		else
			entity.setUnindexedProperty(this.path, value);
	}
}
