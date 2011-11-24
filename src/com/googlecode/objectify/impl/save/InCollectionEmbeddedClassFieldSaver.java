package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Unindex;
import com.googlecode.objectify.impl.Path;

/**
 * <p>Saver which knows how to save simple embedded classes, not arrays or collections.</p>
 * 
 * <p>If the field is null, store a null</p>
 */
public class InCollectionEmbeddedClassFieldSaver extends FieldSaver
{
	/** Used to actually save the object in the field */
	InCollectionEmbeddedClassSaver classSaver;
	
	/**
	 * @param ignoreClassIndexing is for the class that contains this embedded class field, not the embedded class.
	 */
	public InCollectionEmbeddedClassFieldSaver(ObjectifyFactory fact, Class<?> examinedClass, Field field, boolean ignoreClassIndexing)
	{
		super(examinedClass, field, ignoreClassIndexing, false);
		
		boolean ignoreClassIndexingAnnotations =
			this.field.isAnnotationPresent(Index.class) || this.field.isAnnotationPresent(Unindex.class);
		
		// Must pass the indexed from our member field, not from the inherited value
		this.classSaver = new InCollectionEmbeddedClassSaver(fact, field.getType(), ignoreClassIndexingAnnotations);
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.save.FieldSaver#saveValue(java.lang.Object, com.google.appengine.api.datastore.Entity, boolean)
	 */
	@Override
	public void saveValue(Object value, Entity entity, Path path, boolean index)
	{
		if (value == null)
		{
			this.setEntityProperty(entity, null, path, index);
		}
		else
		{
			this.classSaver.save(value, entity, path, index);
		}
	}
}
