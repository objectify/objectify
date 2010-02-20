package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>Save which discovers how to save a class, either root pojo or embedded.</p>
 */
public class ClassSaver implements Saver
{
	/** Classes are composed of fields, each of which could be a LeafSaver or an EmbeddedArraySaver etc */
	List<Saver> fieldSavers = new ArrayList<Saver>();

	/**
	 * Creates a ClassSaver for a root entity pojo class.  If nothing is specified otherwise, all
	 * fields default to indexed
	 */
	public ClassSaver(ObjectifyFactory factory, Class<?> rootClazz)
	{
		this(factory, null, rootClazz, true, false, false);
	}
	
	/**
	 * @param pathPrefix is the entity path to this class, ie "field1.field2" for an embedded field1 containing a field2
	 *  of the type of this class.  The root pathPrefix is null.
	 * @param clazz is the class we want to save.
	 * @param inheritedIndexed is the inherited default for whether fields should be indexed or not.
	 * @param forcedInherit if true, ignores local @Indexed or @Unindexed and uses the inherited value
	 * @param collectionize causes all leaf setters to create and append to a simple list of
	 *  values rather than to set the value directly.  After we hit an embedded array or
	 *  an embedded collection, all subsequent savers are collectionized.
	 */
	public ClassSaver(ObjectifyFactory factory, String pathPrefix, Class<?> clazz, boolean inheritedIndexed, boolean forcedInherit, boolean collectionize)
	{
		if (clazz.isAnnotationPresent(Indexed.class) && clazz.isAnnotationPresent(Unindexed.class))
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same class: " + clazz.getName());
		
		// If we aren't forcing indexed inheritance because we're an embedded class and the
		// field had an indexing annotation, we can look at our own indexing annotations.
		if (!forcedInherit)
		{
			if (clazz.isAnnotationPresent(Indexed.class))
				inheritedIndexed = true;
			else if (clazz.isAnnotationPresent(Unindexed.class))
				inheritedIndexed = false;
		}
		
		List<Field> fields = TypeUtils.getPesistentFields(clazz);

		for (Field field: fields)
		{
			if (TypeUtils.isEmbedded(field))
			{
				if (field.getType().isArray())
				{
					Saver saver = new EmbeddedArrayFieldSaver(factory, pathPrefix, field, inheritedIndexed, collectionize);
					this.fieldSavers.add(saver);
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedCollectionFieldSaver(factory, pathPrefix, field, inheritedIndexed, collectionize);
					this.fieldSavers.add(saver);
				}
				else	// basic class
				{
					Saver saver = new EmbeddedClassFieldSaver(factory, pathPrefix, field, inheritedIndexed, collectionize);
					this.fieldSavers.add(saver);
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Saver saver = new LeafFieldSaver(factory, pathPrefix, field, inheritedIndexed, collectionize);
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
