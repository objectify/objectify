package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 */
class FieldSerializer implements Serializer
{
	private final ObjectifyFactory factory;
	private final String name;
	private final Field field;
	private final String oldname;
	private final boolean listProperty;
	private final boolean indexed;
	private Class<?> fieldType;
	private Class<?> fieldComponentType;

	public FieldSerializer(ObjectifyFactory factory, String name, String oldname, Field field, boolean indexed, boolean listProperty)
	{
		this.factory = factory;
		this.name = name;
		this.field = field;
		this.fieldType = field.getType();
		this.fieldComponentType = TypeUtils.getComponentType(fieldType, field.getGenericType());
		this.oldname = oldname;
		this.indexed = indexed;
		this.listProperty = listProperty;
	}

	public void loadIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
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
	public void loadIntoList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
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
	@Override
	public void saveObject(Entity ent, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		value = convertToDatastore(value);
		if (value == null && listProperty)
		{
			return; // rule: omit when null or empty
		}

		if (!indexed)
			ent.setUnindexedProperty(name, value);
		else
			ent.setProperty(name, value);
		// TODO: Add warning if the field is indexed but we have converted it to Text (which is always unindexed).
	}

	@Override
	public void saveIntoList(ListPropertyMap entity, int i, Object obj) throws IllegalAccessException
	{
		Object value = field.get(obj);
		value = convertToDatastore(value);
		entity.setValue(name, i, value);
	}

	@Override
	public void prepareListForSave(ListPropertyMap arrays)
	{
		arrays.createArray(name, indexed);
	}

	@Override
	public void verify()
	{
	}

	/**
	 * Converts the value into an object suitable for storing in the datastore.
	 */
	public Object convertToDatastore(Object value)
	{
		if (value == null)
		{
			return null;
		}
		else if (value instanceof String)
		{
			// Check to see if it's too long and needs to be Text instead
			if (((String) value).length() > 500)
				return new Text((String) value);
			else
				return value;
		}
		else if (value instanceof Enum<?>)
		{
			return value.toString();
		}
		else if (value.getClass().isArray())
		{
			// The datastore cannot persist arrays, but it can persist ArrayList
			int length = Array.getLength(value);
			if (length == 0)
			{
				return null; // rule: omit when empty
			}
			ArrayList<Object> list = new ArrayList<Object>(length);

			for (int i = 0; i < length; i++)
				list.add(convertToDatastore(Array.get(value, i)));

			return list;
		}
		else if (value instanceof Collection<?>)
		{
			Collection<?> collection = (Collection<?>) value;
			if (collection.isEmpty())
			{
				return null; // rule: omit when empty
			}
			// All collections get turned into a List that preserves the order.  We must
			// also be sure to convert anything contained in the collection
			ArrayList<Object> list = new ArrayList<Object>(collection.size());

			for (Object obj : collection)
				list.add(convertToDatastore(obj));

			return list;
		}
		else if (value instanceof Key<?>)
		{
			return factory.oKeyToRawKey((Key<?>) value);
		}
		else
		{
			// Usually we just want to return the value
			return value;
		}
	}
}
