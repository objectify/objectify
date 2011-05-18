package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;
import com.googlecode.objectify.impl.conv.Conversions;


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
	public ClassSaver(Conversions conv, Class<?> rootClazz)
	{
		this(conv, rootClazz, false, false, false);
	}
	
	/**
	 * @param clazz is the class we want to save.
	 * @param ignoreClassIndexing will cause the saver to ignore the @Indexed or @Unindexed annotations on the class
	 *  (ie we are processing an @Embedded class and the field itself was annotated)
	 * @param collectionize causes all leaf setters to create and append to a simple list of
	 *  values rather than to set the value directly.  After we hit an embedded array or
	 *  an embedded collection, all subsequent savers are collectionized.
	 * @param embedding is true if we are embedding a class.  Causes @Id and @Parent fields to be treated as normal
	 *  persistent fields rather than real ids.
	 */
	public ClassSaver(Conversions conv, Class<?> clazz, boolean ignoreClassIndexing, boolean collectionize, boolean embedding)
	{
		List<FieldMetadata> fields = TypeUtils.getPesistentFields(clazz, embedding);

		for (FieldMetadata metadata: fields)
		{
			Field field = metadata.field;
			
			if (TypeUtils.isEmbedded(field))
			{
				if (field.getType().isArray())
				{
					Saver saver = new EmbeddedArrayFieldSaver(conv, clazz, field, ignoreClassIndexing, collectionize);
					this.fieldSavers.add(saver);
				}
				else if (Map.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedMapSaver(conv, clazz, field, ignoreClassIndexing, collectionize);
					this.fieldSavers.add(saver);
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedCollectionFieldSaver(conv, clazz, field, ignoreClassIndexing, collectionize);
					this.fieldSavers.add(saver);
				}
				else	// basic class
				{
					Saver saver = new EmbeddedClassFieldSaver(conv, clazz, field, ignoreClassIndexing, collectionize);
					this.fieldSavers.add(saver);
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Saver saver = new LeafFieldSaver(conv, clazz, field, ignoreClassIndexing, collectionize);
				this.fieldSavers.add(saver);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.Saver#save(java.lang.Object, com.google.appengine.api.datastore.Entity)
	 */
	@Override
	public void save(Object pojo, Entity entity, Path path, boolean index)
	{
		for (Saver fieldSaver: this.fieldSavers)
			fieldSaver.save(pojo, entity, path, index);
	}
}
