package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;
import com.googlecode.objectify.impl.conv.ConverterRegistry;


/**
 * <p>Save which discovers how to save a class, either root pojo or simple embedded.  Does NOT
 * cover classes embedded in collections.</p>
 */
public class ClassSaver implements Saver
{
	/** Classes are composed of fields, each of which could be a LeafSaver or an EmbeddedArraySaver etc */
	List<Saver> fieldSavers = new ArrayList<Saver>();
	
	/**
	 * Creates a ClassSaver for a root entity pojo class.  If nothing is specified otherwise, all
	 * fields default to indexed
	 */
	public ClassSaver(ObjectifyFactory fact, Class<?> rootClazz)
	{
		this(fact, rootClazz, false, false);
	}
	
	/**
	 * @param clazz is the class we want to save.
	 * @param ignoreClassIndexing will cause the saver to ignore the @Indexed or @Unindexed annotations on the class
	 *  (ie we are processing an @Embedded class and the field itself was annotated)
	 * @param embedding is true if we are embedding a class.  Causes @Id and @Parent fields to be treated as normal
	 *  persistent fields rather than real ids.
	 */
	public ClassSaver(ObjectifyFactory fact, Class<?> clazz, boolean ignoreClassIndexing, boolean embedding)
	{
		List<FieldMetadata> fields = TypeUtils.getPesistentFields(clazz, embedding);

		for (FieldMetadata metadata: fields)
		{
			Field field = metadata.field;
			
			if (TypeUtils.isEmbed(field))
			{
				if (field.getType().isArray())
				{
					Saver saver = new EmbeddedArrayFieldSaver(fact, clazz, field, ignoreClassIndexing);
					this.fieldSavers.add(saver);
				}
				else if (Map.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedMapFieldSaver(fact, clazz, field, ignoreClassIndexing);
					this.fieldSavers.add(saver);
				}
				else if (Collection.class.isAssignableFrom(field.getType()))
				{
					Saver saver = new EmbeddedCollectionFieldSaver(fact, clazz, field, ignoreClassIndexing);
					this.fieldSavers.add(saver);
				}
				else	// basic class
				{
					Saver saver = new EmbeddedClassFieldSaver(fact, clazz, field, ignoreClassIndexing);
					this.fieldSavers.add(saver);
				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Saver saver = new LeafFieldSaver(fact, clazz, field, ignoreClassIndexing, false);
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
