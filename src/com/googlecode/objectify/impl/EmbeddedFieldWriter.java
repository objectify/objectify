package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedFieldWriter implements Writer
{
	private final Field field;
	private final String name;
	private final boolean indexed;
	private final ClassSerializer subinfo;

	EmbeddedFieldWriter(Field field, String name, boolean indexed, ClassSerializer subinfo)
	{
		this.field = field;
		this.name = name;
		this.indexed = indexed;
		this.subinfo = subinfo;
	}

	@Override
	public void addtoEntity(Entity ent, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		if (value == null)
		{
			// emit null embedded indicator
			if (indexed)
			{
				ent.setProperty(name, null);
			}
			else
			{
				ent.setUnindexedProperty(name, null);
			}
			return;
		}

		for (Writer fw : subinfo.getWriteables())
		{
			fw.addtoEntity(ent, value);
		}
	}

	public void initArray(ListPropertyMap arrays)
	{
		//TODO omit/null?
		for (Writer fw : subinfo.getWriteables())
		{
			fw.initArray(arrays);
		}
	}

	public void addtoArray(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		//TODO omit/null?
		Object value = field.get(obj);
		for (Writer fw : subinfo.getWriteables())
		{
			fw.addtoArray(entity, i, value);
		}
	}
}
