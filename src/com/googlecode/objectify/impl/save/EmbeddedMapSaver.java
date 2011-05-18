package com.googlecode.objectify.impl.save;

import java.lang.reflect.Field;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.conv.Conversions;
import com.googlecode.objectify.impl.conv.ConverterSaveContext;

/**
 * Saves entries of string-keyed maps into the Entity, using their key as an intermediate field name.
 */
public class EmbeddedMapSaver extends FieldSaver implements ConverterSaveContext
{

	boolean ignoreClassIndexing;
	ClassSaver nestedSaver;
	Conversions conversions;

	public EmbeddedMapSaver(Conversions conv, Class<?> examinedClass, Field field, boolean ignoreClassIndexing,
			boolean collectionize)
	{
		super(examinedClass, field, ignoreClassIndexing, collectionize);
		this.conversions = conv;
		this.ignoreClassIndexing = ignoreClassIndexing;
		Class<?> valueType = TypeUtils.getMapValueType(field.getGenericType());
		if (Object.class.equals(valueType))
		{
			this.nestedSaver = null;
		}
		else
		{
			this.nestedSaver = new ClassSaver(conv, valueType, ignoreClassIndexing, collectionize, true);
		}
	}

	@Override
	protected void saveValue(Object object, Entity entity, Path path, boolean index)
	{
		@SuppressWarnings("unchecked")
		// we know it's a map
		Map<String, ?> map = (Map<String, ?>) object;
		for (Map.Entry<String, ?> entry : map.entrySet())
		{
			String key = entry.getKey();
			Object value = entry.getValue();
			if (key == null)
			{
				throw new IllegalStateException("Cannot store null keys (at " + path + " for " + field + ")");
			}
			if (key.contains("."))
			{
				throw new IllegalStateException("Cannot store keys with '.' in their name: " + key + " at " + path + " for " + field);
			}
			if (value == null)
			{
				// always ignore null entries in maps. this breaks "containsKey", but seems like the right thing anyway.
				continue;
			}
			Path subPath = path.extend(key);
			if (nestedSaver == null)
			{
				// we're directly operating on datastore objects, without another saver
				setEntityProperty(entity, this.conversions.forDatastore(value, this), subPath, index);
			}
			else
			{
				nestedSaver.save(value, entity, subPath, index);
			}
		}
	}

	@Override
	public boolean inEmbeddedCollection()
	{
		return false;
	}

	@Override
	public Field getField()
	{
		return field;
	}
}
