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
	public EmbeddedClassFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean inheritedUnindexed, boolean collectionize)
	{
		super(pathPrefix, field, inheritedUnindexed);
		
		// Must pass the indexed from our member field, not from the inherited value
		this.classSaver = new ClassSaver(fact, this.path, field.getType(), this.indexed, this.forcedInherit, collectionize);
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
			this.setEntityProperty(entity, null);
		}
		else
		{
			this.classSaver.save(embeddedPojo, entity);
		}
	}
}
