package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Saver which knows how to save simple embedded classes, not arrays or collections.</p>
 * 
 * <p>If the field is null, store a null</p>
 */
public class EmbeddedClassFieldSaver extends FieldSaver
{
	/** Used to actually save the object in the field */
	ClassSaver classSaver;
	
	/**
	 */
	public EmbeddedClassFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean forceUnindexed, boolean collectionize)
	{
		super(pathPrefix, field, forceUnindexed);
		
		this.classSaver = new ClassSaver(fact, this.path, field.getType(), this.unindexed, collectionize);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public void save(Object pojo, Entity entity)
	{
		Object embeddedPojo = TypeUtils.field_get(this.field, pojo);
		if (embeddedPojo == null)
		{
			if (this.unindexed)
				entity.setUnindexedProperty(this.path, null);
			else
				entity.setProperty(this.path, null);
		}
		else
		{
			this.classSaver.save(embeddedPojo, entity);
		}
	}
}
