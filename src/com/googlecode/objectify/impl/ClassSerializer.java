package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.OldName;

/**
 * <p>Translates properties between typed entity objects and raw datastore Entities
 * based on a description of field/property mappings set on this class.</p>
 * 
 * <p>ClassSerializers may be hierarchically configured in order to serialize an object graph.</p>
 * 
 * <p>Mappings are added using {@link #addField}, {@link #addEmbeddedField}, {@link #addEmbeddedArrayField}, {@link #addMethod}.</p>
 * 
 * <p>This class does not translate @Ids or @Parents - those are part of the key and are
 * assumed to be already set.  This class translates normal properties only.</p>
 */
public class ClassSerializer implements Populator
{
	/**
	 * The fields we persist, not including the @Id or @Parent fields
	 */
	private final Set<Writer> writeables = new HashSet<Writer>();
	private Map<String, Populator> populators = new HashMap<String, Populator>();

	private final ObjectifyFactory factory;

	/** the concrete class this ClassSerializer knows how to serialize. */
	private final Class<?> type;

	public ClassSerializer(ObjectifyFactory factory, Class<?> type)
	{
		this.factory = factory;
		this.type = type;
	}

	/**
	 * Add a field to serialize on this ClassSerializer. This field must be of a core data type, or a
	 * Collection/List of core data types.
	 *
	 * @param field a field on the class that we know how to serialize
	 * @param name the property name to look for in the datastore
	 * @param listProperty if this property represents a declared Collection or array
	 * @param indexed should this field be indexed in the datastore
	 */
	public void addField(Field field, String name, boolean listProperty, boolean indexed)
	{
		FieldWriter writer = new FieldWriter(factory, field, name, listProperty, indexed);
		writeables.add(writer);

		OldName old = field.getAnnotation(OldName.class);
		String oldName = old == null ? null : old.value();

		FieldPopulator ri = new FieldPopulator(factory, name, oldName, field, listProperty);
		if (this.populators.put(name, ri) != null)
			throw new IllegalStateException(
					"Data property name '" + name + "' is duplicated in hierarchy of " +
							type.getName() + ". Check for conflicting fields.");
	}

	/**
	 * Add an @Embedded field to serialize on this ClassSerializer.
	 * For example:
	 * <pre>@Embedded Person mayor</pre>
	 *
	 * @param field a field on this ClassSerializer
	 * @param name the prefix of property names to look for in the datastore
	 * @param indexed should this field be indexed in the datastore
	 * @return the new serializer for @Embedded class
	 */
	public ClassSerializer addEmbeddedField(Field field, String name, boolean indexed)
	{
		// TODO oldname support
		ClassSerializer subinfo = new ClassSerializer(factory, field.getType());

		TypeUtils.checkForNoArgConstructor(field.getType());
		EmbeddedFieldWriter writer = new EmbeddedFieldWriter(field, name, indexed, subinfo);
		writeables.add(writer);

		EmbeddedPopulator ri = new EmbeddedPopulator(field, name, subinfo);
		if (populators.put(name, ri) != null)
		{
			throw new IllegalStateException(
					"Embedded property name '" + name + "' is duplicated in hierarchy of " +
							type.getName() + ". Check for conflicting fields.");
		}

		return subinfo;
	}

	/**
	 * Add an @Embedded array/Collection field to serialize on this ClassSerializer
	 * For example:
	 * <pre>@Embedded Person[] folk</pre>
	 *
	 * @param field a field on this ClassSerializer
	 * @param name the prefix of property names to look for in the datastore
	 * @return the new serializer for @Embedded class
	 */
	public ClassSerializer addEmbeddedArrayField(Field field, String name)
	{
		// TODO oldname support
		ClassSerializer subinfo = new ClassSerializer(factory, field.getType());

		EmbeddedArrayWriter writer = new EmbeddedArrayWriter(field, subinfo);
		writeables.add(writer);

		EmbeddedArrayPopulator ri = new EmbeddedArrayPopulator(field, subinfo);
		if (populators.put(name, ri) != null)
		{
			throw new IllegalStateException(
					"Embedded property array/Collection name '" + name + "' is duplicated in hierarchy of " +
							type.getName() + ". Check for conflicting fields.");
		}

		return subinfo;
	}

	/**
	 * Add an @OldName method to use during deserialization.
	 * @param method a method on this ClassSerializer
	 * @param oldname the property name to look for in the datastore
	 */
	public void addMethod(Method method, String oldname)
	{
		if (populators.put(oldname, new MethodPopulator(factory, oldname, method)) != null)
			throw new IllegalStateException(
					"@OldName method for data property '" + oldname + "' is duplicated in hierarchy of " +
							type.getName() + ". Check for conflicting fields.");
	}

	public void populateIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
	{
		for (Populator ri : populators.values())
		{
			ri.populateIntoObject(ent, dest);
		}
	}

	public void populateFromList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		for (Populator ri : populators.values())
		{
			ri.populateFromList(ent, dests);
		}
	}

	/**
	 * Converts an object to a datastore Entity.
	 */
	public void toEntity(Entity ent, Object obj)
	{
		try
		{
			for (Writer fw : writeables)
			{
				fw.addtoEntity(ent, obj);
			}
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	Set<Writer> getWriteables()
	{
		return writeables;
	}

	public Class<?> getType()
	{
		return type;
	}
}
