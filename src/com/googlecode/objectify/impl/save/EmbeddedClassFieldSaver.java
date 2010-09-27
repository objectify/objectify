package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;

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
	public EmbeddedClassFieldSaver(ObjectifyFactory fact, String pathPrefix, Class<?> examinedClass, Field field, boolean collectionize)
	{
		super(pathPrefix, examinedClass, field, collectionize);
		
		boolean ignoreClassIndexingAnnotations =
			this.field.isAnnotationPresent(Indexed.class) || this.field.isAnnotationPresent(Unindexed.class);
		
		// Must pass the indexed from our member field, not from the inherited value
		this.classSaver = new ClassSaver(fact, this.path, field.getType(), ignoreClassIndexingAnnotations, collectionize, true);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.FieldSaver#saveValue(java.lang.Object, com.google.appengine.api.datastore.Entity, boolean)
	 */
	@Override
	public void saveValue(Object value, Entity entity, boolean index)
	{
		if (value == null)
		{
			this.setEntityProperty(entity, null, index);
		}
		else
		{
			this.classSaver.save(value, entity, index);
		}
	}
}
