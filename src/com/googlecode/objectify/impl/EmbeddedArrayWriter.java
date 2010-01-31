package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedArrayWriter implements Writer
{
	private final Field field;
	private final ClassSerializer subinfo;

	EmbeddedArrayWriter(Field field, ClassSerializer subinfo)
	{
		this.field = field;
		this.subinfo = subinfo;
	}

	@Override
	public void addtoEntity(Entity ent, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		if (value == null)
			return; // omit property for null Collections

		Object[] array = (Object[]) value;

		if (array.length == 0)
			return; // omit property for empty Collections

		ListPropertyMap tmpEntity = new ListPropertyMap(array.length);

		for (Writer fw : subinfo.getWriteables())
		{
			fw.initArray(tmpEntity);
			for (int i = 0; i < array.length; i++)
			{
				fw.addtoArray(tmpEntity, i, array[i]);
			}
		}
		tmpEntity.addToEntity(ent);
	}

	@Override
	public void addtoArray(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		// can't occur, we don't support arrays of arrays
		throw new RuntimeException();
	}

	@Override
	public void initArray(ListPropertyMap arrays)
	{
		// can't occur, we don't support arrays of arrays
		throw new RuntimeException();
	}
}
