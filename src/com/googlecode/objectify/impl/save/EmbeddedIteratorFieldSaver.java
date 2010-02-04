package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Base class for EmbeddedArrayFieldSaver and EmbeddedCollectionFieldSaver
 * that handles most of the logic.  The subclasses need only understand how to
 * get the component type and how to make an iterator.</p>
 * 
 * <p>TODO:  null handling</p>
 */
abstract public class EmbeddedIteratorFieldSaver extends FieldSaver
{
	/** Used to actually save the object in the field */
	ClassSaver classSaver;

	/**
	 * @param field must be an array type
	 * @param collectionize must always be false because we cannot nest embedded arrays
	 *  or collections.  This parameter is here so that it is always passed in the code,
	 *  never forgotten, and will always generate the appropriate runtime error.
	 */
	public EmbeddedIteratorFieldSaver(ObjectifyFactory fact, String pathPrefix, Field field, boolean forceUnindexed, boolean collectionize)
	{
		super(pathPrefix, field, forceUnindexed);
		
		if (collectionize)
			throw new IllegalStateException("You cannot nest multiple @Embedded arrays or collections. A second was found at " + field);
		
		// Now we collectionize everything on down
		this.classSaver = new ClassSaver(fact, this.path, this.getComponentType(), this.unindexed, true);
	}
	
	/** Gets the component type of the field */
	abstract protected Class<?> getComponentType();

	/** Gets an iterator from the array or collection passed in */
	abstract protected Iterator<Object> iterator(Object arrayOrCollection);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	final public void save(Object pojo, Entity entity)
	{
		Object arrayOrCollection = TypeUtils.field_get(this.field, pojo);
		if (arrayOrCollection == null)
		{
			//TODO
		}
		else
		{
			Iterator<Object> iterator = this.iterator(arrayOrCollection);
			while (iterator.hasNext())
			{
				Object embeddedPojo = iterator.next();
				
				if (embeddedPojo == null)
				{
					// TODO
				}
				else
				{
					this.classSaver.save(embeddedPojo, entity);
				}
			}
		}
	}
}
