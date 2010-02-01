package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedArraySerializer implements Serializer
{
	private final Field field;
	private final ClassSerializer info;

	EmbeddedArraySerializer(Field field, ClassSerializer info)
	{
		this.field = field;
		this.info = info;
	}

	public void loadIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
	{
		ListHolder subDests = new ListHolder(field.getType().getComponentType());
		info.loadIntoList(ent, subDests);

		field.set(dest.get(), subDests.toArray(field.getType().getComponentType()));
	}

	@Override
	public void loadIntoList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		// should not be possible to reach here, you can't have arrays of arrays
		throw new RuntimeException();
	}
	@Override
	public void saveObject(Entity ent, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		if (value == null)
			return; // omit property for null Collections

		Object[] array = (Object[]) value;

		if (array.length == 0)
			return; // omit property for empty Collections

		ListPropertyMap tmpEntity = new ListPropertyMap(array.length);

		for (Serializer fw : info.getSerializers())
		{
			fw.prepareListForSave(tmpEntity);
			for (int i = 0; i < array.length; i++)
			{
				fw.saveIntoList(tmpEntity, i, array[i]);
			}
		}
		tmpEntity.addToEntity(ent);
	}

	@Override
	public void verify()
	{
		info.verify();
	}

	@Override
	public void saveIntoList(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		// can't occur, we don't support arrays of arrays
		throw new RuntimeException();
	}

	@Override
	public void prepareListForSave(ListPropertyMap arrays)
	{
		// can't occur, we don't support arrays of arrays
		throw new RuntimeException();
	}

}
