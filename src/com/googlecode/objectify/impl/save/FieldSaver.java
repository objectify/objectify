package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
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
	}
	
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
