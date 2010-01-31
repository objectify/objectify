package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;

import java.lang.reflect.Field;

/**
 */
class EmbeddedArrayPopulator implements Populator
{
	private final Field field;
	private final ClassSerializer info;

	EmbeddedArrayPopulator(Field field, ClassSerializer info)
	{
		this.field = field;
		this.info = info;
	}

	public void populateIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
	{
		ListHolder subDests = new ListHolder(field.getType().getComponentType());
		info.populateFromList(ent, subDests);

		field.set(dest.get(), subDests.toArray(field.getType().getComponentType()));
	}

	@Override
	public void populateFromList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		// should not be possible to reach here, you can't have arrays of arrays
		throw new RuntimeException();
	}

}
