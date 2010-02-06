package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Save which discovers how to save a class, either root pojo or embedded.</p>
 */
public class ClassSaver implements Saver
{
	/** Classes are composed of fields, each of which could be a LeafSaver or an EmbeddedArraySaver etc */
	List<Saver> fieldSavers = new ArrayList<Saver>();

	/** Creates a ClassSaver for a root entity pojo class */
	public ClassSaver(ObjectifyFactory factory, Class<?> rootClazz)
	{
		this(factory, null, rootClazz, false, false);
	}
	
	/**
	 * @param collectionize causes all leaf setters to create and append to a simple list of
	 *  values rather than to set the value directly.  After we hit an embedded array or
	 *  an embedded collection, all subsequent savers are collectionized.
	 */
	public ClassSaver(ObjectifyFactory factory, String pathPrefix, Class<?> clazz, boolean forceUnindexed, boolean collectionize)
	{
		List<Field> fields = TypeUtils.getPesistentFields(clazz);
		for (Field field: fields)
		{
			if (TypeUtils.isEmbedded(field))
			{
				if (field.getType().isArray())
				{
					Saver saver = new EmbeddedArrayFieldSaver(factory, pathPrefix, field, forceUnindexed, collectionize);
					this.fieldSavers.add(saver);
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedCollectionFieldSaver(factory, pathPrefix, field, forceUnindexed, collectionize);
					this.fieldSavers.add(saver);
				}
				else	// basic class
				{
					Saver saver = new EmbeddedClassFieldSaver(factory, pathPrefix, field, forceUnindexed, collectionize);
					this.fieldSavers.add(saver);
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Saver saver = new LeafFieldSaver(factory, pathPrefix, field, forceUnindexed, collectionize);
				this.fieldSavers.add(saver);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public void save(Object pojo, Entity entity)
	{
		for (Saver fieldSaver: this.fieldSavers)
			fieldSaver.save(pojo, entity);
	}
}
