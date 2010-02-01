package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedFieldSerializer implements Serializer
{
	private final Field field;
	private final String name;
	private final boolean indexed;
	private final ClassSerializer info;

	EmbeddedFieldSerializer(Field field, String name, boolean indexed, ClassSerializer info)
	{
		this.field = field;
		this.name = name;
		this.indexed = indexed;
		this.info = info;
	}

	public void loadIntoObject(Entity ent, ObjectHolder dest) throws InstantiationException, IllegalAccessException
	{
		ObjectHolder subHolder = new ObjectHolder(info.getType());
		info.loadIntoObject(ent, subHolder);

		if (ent.hasProperty(name) && (ent.getProperty(name)) == null)
		{
			field.set(dest.get(), null);
		}
		else
		{
			// if no properties are found at all, the leave the field as its default value
			if (subHolder.wasCreated())
			{
				field.set(dest.get(), subHolder.get());
			}
		}
	}

	public void loadIntoList(Entity ent, ListHolder dests) throws InstantiationException, IllegalAccessException
	{
		ListHolder subdests = new ListHolder(info.getType());
		info.loadIntoList(ent, subdests);

		if (subdests.size() == 0)
		{
			//TODO nothing was found
		}
		else
		{
			dests.initToSize(subdests.size());
			for (int i = 0; i < dests.size(); i++)
			{
				Object value = subdests.get(i);
				field.set(dests.get(i), value);
			}
		}
	}
	@Override
	public void saveObject(Entity ent, Object obj) throws IllegalAccessException
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

		for (Serializer fw : info.getSerializers())
		{
			fw.saveObject(ent, value);
		}
	}

	public void prepareListForSave(ListPropertyMap arrays)
	{
		//TODO omit/null?
		for (Serializer fw : info.getSerializers())
		{
			fw.prepareListForSave(arrays);
		}
	}

	public void saveIntoList(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		//TODO omit/null?
		Object value = field.get(obj);
		for (Serializer fw : info.getSerializers())
		{
			fw.saveIntoList(entity, i, value);
		}
	}

	@Override
	public void verify()
	{
		info.verify();
	}
}
