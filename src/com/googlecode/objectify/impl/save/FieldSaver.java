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
	boolean unindexed;
	
	/** */
	public FieldSaver(String pathPrefix, Field field, boolean unindexedByDefault)
	{
		this.field = field;
		
		this.path = TypeUtils.extendPropertyPath(pathPrefix, field.getName());
		this.unindexed = (unindexedByDefault || field.isAnnotationPresent(Unindexed.class)) && !field.isAnnotationPresent(Indexed.class);
	}
	
	/** 
	 * Sets property on the entity correctly for the values of this.path and this.unindexed.
	 */
	protected void setEntityProperty(Entity entity, Object value)
	{
		if (this.unindexed)
			entity.setUnindexedProperty(this.path, value);
		else
			entity.setProperty(this.path, value);
	}
}
