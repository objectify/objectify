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
public class ClassSerializer
{
	/**
	 * The fields we serialize, not including the @Id or @Parent fields
	 */
	private final Map<String, Serializer> serializers = new HashMap<String, Serializer>();
	private final Map<String, MethodPopulator> methodLoaders = new HashMap<String, MethodPopulator>();

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
		OldName old = field.getAnnotation(OldName.class);
		String oldName = old == null ? null : old.value();

		FieldSerializer ri = new FieldSerializer(factory, name, oldName, field, indexed, listProperty);
		if (this.serializers.put(name, ri) != null)
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

		EmbeddedFieldSerializer ri = new EmbeddedFieldSerializer(field, name, indexed, subinfo);
		if (serializers.put(name, ri) != null)
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

		EmbeddedArraySerializer ri = new EmbeddedArraySerializer(field, subinfo);
		if (serializers.put(name, ri) != null)
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
		if (methodLoaders.put(oldname, new MethodPopulator(factory, oldname, method)) != null)
			throw new IllegalStateException(
					"@OldName method for data property '" + oldname + "' is duplicated in hierarchy of " +
							type.getName() + ". Check for conflicting fields.");
	}

	/**
	 * Call to check if the way this class has been configured confirms to all the rules.
	 */
	public void verify() {
		Set<String> names = new HashSet<String>(serializers.keySet());
		names.retainAll(methodLoaders.keySet());
		if (!names.isEmpty())
		{
			throw new IllegalArgumentException("Duplicate property name between field and @OldName method '"
					+ names.iterator().next() + "' for type " + type.getName() + ". Check for conflicting fields.");
		}

		for (Serializer serializer : serializers.values())
		{
			serializer.verify();
		}
	}

	

	public void loadIntoObject(Entity ent, ObjectHolder dest) throws IllegalAccessException, InstantiationException
	{
		for (Serializer ri : serializers.values())
		{
			ri.loadIntoObject(ent, dest);
		}
		for (MethodPopulator ri : methodLoaders.values())
		{
			ri.populateIntoObject(ent, dest);
		}
	}

	void loadIntoList(Entity ent, ListHolder dests) throws IllegalAccessException, InstantiationException
	{
		for (Serializer ri : serializers.values())
		{
			ri.loadIntoList(ent, dests);
		}
	}

	/**
	 * Converts an object to a datastore Entity.
	 */
	public void toEntity(Entity ent, Object obj)
	{
		try
		{
			for (Serializer fw : serializers.values())
			{
				fw.saveObject(ent, obj);
			}
		}
		catch (IllegalAccessException e)
		{
			throw new RuntimeException(e);
		}
	}

	Iterable<Serializer> getSerializers()
	{
		return serializers.values();
	}

	public Class<?> getType()
	{
		return type;
	}
}
