package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 */
class FieldPopulator implements Populator
{
	private final ObjectifyFactory factory;
	private final String name;
	private final Field field;
	private final String oldname;
	private final boolean listProperty;
	private Class<?> fieldType;
	private Class<?> fieldComponentType;

	public FieldPopulator(ObjectifyFactory factory, String name, String oldname, Field field, boolean listProperty)
	{
		this.factory = factory;
		this.name = name;
		this.field = field;
		this.fieldType = field.getType();
		this.fieldComponentType = TypeUtils.getComponentType(fieldType, field.getGenericType());
		this.oldname = oldname;
		this.listProperty = listProperty;
	}

	public void populateIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
	{
		String propname = whichPropname(ent);
		Object value;
		if (propname == null)
		{
			if (listProperty)
			{
				value = new ArrayList<Object>(); // Rule: load null/absent as empty list
			}
			else
			{
				return;
			}
		}
		else
		{
			value = ent.getProperty(propname);
			if (listProperty && (value == null))
			{
				value = new ArrayList<Object>(); // Rule: load null/absent as empty list
			}
		}
		value = TypeUtils.convertFromDatastore(factory, value, fieldType, fieldComponentType);
		field.set(dest.get(), value);
	}

	@SuppressWarnings("unchecked")
	public void populateFromList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		String propname = whichPropname(ent);
		if (propname == null)
		{
			return;
		}

		Object o = ent.getProperty(propname);
		if (o == null || !(o instanceof List<?>)) {
			return; // TODO is this what we want to do, test for it
		}
		List<Object> list = (List<Object>) o;
		dests.initToSize(list.size());
		for (int i = 0; i < dests.size(); i++)
		{
			Object value = list.get(i);
			value = TypeUtils.convertFromDatastore(factory, value, fieldType, fieldComponentType);
			field.set(dests.get(i), value);
		}
	}

	private String whichPropname(Entity ent)
	{
		String propname;

		boolean hasold = ent.hasProperty(oldname);
		if (ent.hasProperty(name))
		{
			if (oldname != null && hasold)
			{
				throw new IllegalStateException("Tried to set '" + name + "' twice since " +
						oldname + " also existed; check @OldName annotations");
			}
			propname = name;
		}
		else
		{
			if (!hasold)
				propname = null; // skip, leave obj with default value
			else
				propname = oldname;
		}
		return propname;
	}
}
