package com.googlecode.objectify.impl.load;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;
import com.googlecode.objectify.impl.conv.StandardConversions;
import com.googlecode.objectify.impl.save.Path;


/**
 * <p>Loader which discovers how to load a class, either root pojo or embedded.</p>
 */
public class ClassLoadr implements Loader
{
	/** Classes are composed of fields, duh */
	List<FieldLoader> fieldLoaders = new ArrayList<FieldLoader>();
	
	/**
	 * @param clazz is the class we want to load.
	 * @param embedding is true if we are embedding a class.  Causes @Id and @Parent fields to be treated as normal
	 *  persistent fields rather than real ids.
	 */
	public ClassLoadr(StandardConversions conv, Class<?> clazz, boolean embedding)
	{
		List<FieldMetadata> fields = TypeUtils.getPesistentFields(clazz, embedding);

		for (FieldMetadata metadata: fields)
		{
			Field field = metadata.field;
			
			if (TypeUtils.isEmbedded(field))
			{
//				if (field.getType().isArray())
//				{
//					Loader loader = new EmbeddedArrayFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else if (Map.class.isAssignableFrom(field.getType()))
//				{
//					Loader loader = new EmbeddedMapLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else if (Collection.class.isAssignableFrom(field.getType()))
//				{
//					Loader loader = new EmbeddedCollectionFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
//				else	// basic class
//				{
//					Loader loader = new EmbeddedClassFieldLoader(conv, clazz, field);
//					this.fieldLoaders.add(saver);
//				}
			}
			else	// not embedded, so we're at a leaf object (including arrays and collections of basic types)
			{
				// Add a leaf saver
				Loader saver = new LeafFieldLoader(conv, clazz, field);
				this.fieldLoaders.add(saver);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.Loader#load(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.save.Path)
	 */
	@Override
	public void load(Object value, Object pojo, Path path)
	{
		EntityNode node = (EntityNode)value;
		
		for (FieldLoader fieldLoader: this.fieldLoaders) {
			Object val = node.get(fieldLoader.getFieldName());
			fieldLoader.load(val, pojo, path);
		}
	}
}
