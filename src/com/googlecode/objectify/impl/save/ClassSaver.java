package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Indexed;
import com.googlecode.objectify.annotation.Unindexed;
import com.googlecode.objectify.condition.Always;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;

/**
 * <p>Save which discovers how to save a class, either root pojo or embedded.</p>
 */
public class ClassSaver implements Saver
{
	/** Classes are composed of fields, each of which could be a LeafSaver or an EmbeddedArraySaver etc */
	List<Saver> fieldSavers = new ArrayList<Saver>();
	
	/** If this is non-null, it means we have a value that should override the current save mode */
	Boolean indexed;

	/**
	 * Creates a ClassSaver for a root entity pojo class.  If nothing is specified otherwise, all
	 * fields default to indexed
	 */
	public ClassSaver(ObjectifyFactory factory, Class<?> rootClazz)
	{
		this(factory, null, rootClazz, false, false, false);
	}
	
	/**
	 * @param pathPrefix is the entity path to this class, ie "field1.field2" for an embedded field1 containing a field2
	 *  of the type of this class.  The root pathPrefix is null.
	 * @param clazz is the class we want to save.
	 * @param ignoreIndexingAnnotations will cause the saver to ignore the @Indexed or @Unindexed annotations on the class
	 * @param collectionize causes all leaf setters to create and append to a simple list of
	 *  values rather than to set the value directly.  After we hit an embedded array or
	 *  an embedded collection, all subsequent savers are collectionized.
	 * @param embedding is true if we are embedding a class.  Causes @Id and @Parent fields to be treated as normal
	 *  persistent fields rather than real ids.
	 */
	public ClassSaver(ObjectifyFactory factory, String pathPrefix, Class<?> clazz, boolean ignoreIndexingAnnotations, boolean collectionize, boolean embedding)
	{
		this.processClassLevelIndexingAnnotations(clazz, ignoreIndexingAnnotations);
		
		List<FieldMetadata> fields = TypeUtils.getPesistentFields(clazz, embedding);

		for (FieldMetadata metadata: fields)
		{
			Field field = metadata.field;
			
			if (TypeUtils.isEmbedded(field))
			{
				if (field.getType().isArray())
				{
					Saver saver = new EmbeddedArrayFieldSaver(factory, pathPrefix, clazz, field, collectionize);
					this.fieldSavers.add(saver);
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedCollectionFieldSaver(factory, pathPrefix, clazz, field, collectionize);
					this.fieldSavers.add(saver);
				}
				else	// basic class
				{
					Saver saver = new EmbeddedClassFieldSaver(factory, pathPrefix, clazz, field, collectionize);
					this.fieldSavers.add(saver);
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Saver saver = new LeafFieldSaver(factory, pathPrefix, clazz, field, collectionize);
				this.fieldSavers.add(saver);
			}
		}
	}

	/**
	 * Recursive function which walks up the superclass hierarchy looking
	 * for Indexed or Unindexed class-level annotations.
	 * 
	 * @param validateOnly when true will only validate the annotations;
	 *  they won't have any actual effect on the indexed or unindexed character of the class.
	 *  This is useful when an @Embedded field was explicitly annotated as indexed/unindexed. 
	 */
	private void processClassLevelIndexingAnnotations(Class<?> clazz, boolean validateOnly)
	{
		// First thing to do is start at the root of the hierarchy
		if (clazz == Object.class)
			return;
		else
			this.processClassLevelIndexingAnnotations(clazz.getSuperclass(), validateOnly);
		
		Indexed indexedAnn = clazz.getAnnotation(Indexed.class);
		Unindexed unindexedAnn = clazz.getAnnotation(Unindexed.class);
		
		if (indexedAnn != null && unindexedAnn != null)
		{
			throw new IllegalStateException("Cannot have @Indexed and @Unindexed on the same class: " + clazz.getName());
		}
		
		if (indexedAnn != null && (indexedAnn.value().length != 1 || indexedAnn.value()[0] != Always.class)
				|| unindexedAnn != null && (unindexedAnn.value().length != 1 || unindexedAnn.value()[0] != Always.class))
		{
			throw new IllegalStateException("Class-level @Indexed and @Unindexed annotations cannot have If conditions: " + clazz.getName());
		}
		
		if (!validateOnly)
		{
			if (indexedAnn != null)
				this.indexed = true;
			else if (unindexedAnn != null)
				this.indexed = false;
		}
	}
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public void save(Object pojo, Entity entity, boolean index)
	{
		if (this.indexed != null)
			index = this.indexed;
		
		for (Saver fieldSaver: this.fieldSavers)
			fieldSaver.save(pojo, entity, index);
	}
}
