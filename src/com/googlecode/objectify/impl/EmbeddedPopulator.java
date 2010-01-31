package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedPopulator implements Populator
{
	private final Field field;
	private final String name;
	private final ClassSerializer info;

	EmbeddedPopulator(Field field, String name, ClassSerializer info)
	{
		this.field = field;
		this.name = name;
		this.info = info;
	}

	public void populateIntoObject(Entity ent, ObjectHolder dest) throws InstantiationException, IllegalAccessException
	{
		ObjectHolder subHolder = new ObjectHolder(info.getType());
		info.populateIntoObject(ent, subHolder);

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

	public void populateFromList(Entity ent, ListHolder dests) throws InstantiationException, IllegalAccessException
	{
		ListHolder subdests = new ListHolder(info.getType());
		info.populateFromList(ent, subdests);

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
}
