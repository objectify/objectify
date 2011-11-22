package com.googlecode.objectify.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.TypeUtils.FieldMetadata;
import com.googlecode.objectify.impl.TypeUtils.MethodMetadata;
import com.googlecode.objectify.impl.conv.StandardConversions;
import com.googlecode.objectify.impl.load.ClassLoadr;
import com.googlecode.objectify.impl.load.EmbeddedArraySetter;
import com.googlecode.objectify.impl.load.EmbeddedClassSetter;
import com.googlecode.objectify.impl.load.EmbeddedCollectionSetter;
import com.googlecode.objectify.impl.load.EmbeddedMapSetter;
import com.googlecode.objectify.impl.load.EmbeddedMultivalueSetter;
import com.googlecode.objectify.impl.load.EmbeddedNullIndexSetter;
import com.googlecode.objectify.impl.load.LeafSetter;
import com.googlecode.objectify.impl.load.RootSetter;
import com.googlecode.objectify.impl.load.Setter;
import com.googlecode.objectify.impl.save.ClassSaver;
import com.googlecode.objectify.impl.save.Path;

/**
 * <p>Class which knows how to load data from Entity to POJO and save data from POJO to Entity.</p>
 * 
 * <p>Note that this class completely ignores @Id and @Parent fields.</p>
 * 
 * <p>To understand this code, you must first understand that a "leaf" value is anything that
 * can be put into the datastore in a single property.  Simple types like String, and Enum,
 * and Key are leaf nodes, but so are Collections and arrays of these basic types.  @Embed
 * values are nonleaf - they branch the persistance graph, producing multiple properties in a
 * datastore Entity.</p>
 * 
 * <p>Also realize that there are two separate dimensions to understand.  Misunderstanding
 * the two related graphs will make this code very confusing:</p>
 * <ul>
 * <li>There is a class graph, which branches at @Embed classes (either simple fields
 * or array/collection fields).  The static analysis code that builds Setters and Savers
 * must traverse this graph.</li>
 * <li>There is an object graph, which branches at @Embed arrays.  The runtime execution
 * code must traverse this graph when setting and saving entities.</li>
 * </ul>
 * 
 * <p>The core structures that operate at runtime are Setters (for loading datastore Entities into
 * typed pojos) and Savers (for saving the fields of typed pojos into datastore Entities).  They are
 * NOT parallel hierarchies, and they work very differently:</p>
 * <ul>
 * <li>When loading, Transmog <em>iterates</em> through the properties of an Entity and for each one calls a Setter
 * that knows how to set this property somewhere deep in the object graph of a typed pojo.  In the case
 * of @Embed arrays and collections, this single collection datastore value will set multipel
 * values in the pojo.  The core data structure is {@code rootSetters}, a map of entity property
 * name to a Setter which knows what to do with that data.</li>
 * <li>When saving, Transmog <em>recurses</em> through the class structure of a pojo (and any embedded objects), calling
 * all relevant Savers to populate the datastore Entity.  The core data structure is {@code rootSaver}, which
 * understands the whole pojo object graph and knows how to translate it into a number of properties
 * on the Entity.</li>
 * </ul>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Transmog<T>
{
	/** The root saver that knows how to save an object of type T */
	ClassSaver rootSaver;
	
	/** The root loader that knows how to load an object of type T */
	ClassLoadr rootLoader;
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, Class<T> clazz)
	{
		this.rootLoader = new ClassLoadr(fact, clazz, false);
		
		this.rootSaver = new ClassSaver(fact, clazz);
	}
	
	/**
	 * Loads the property data in an Entity into a POJO.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param fromEntity is a raw datastore entity
	 * @param toPojo is your typed entity
	 */
	public void load(Entity fromEntity, T toPojo)
	{
		LoadContext context = new LoadContext(toPojo, fromEntity);
		
		for (Map.Entry<String, Object> property: fromEntity.getProperties().entrySet())
		{
			String key = property.getKey();
			Object value = property.getValue();
			loadSingleValue(key, value, toPojo, context);
		}
		
		context.done();
	}

	/**
	 * Loads the single value {@code value} into {@code toPojo} using {@code key} as the key within
	 * the entity. Will use any of {@link #rootSetters} or might delegate through a map setter.
	 * 
	 * @param key the key for this value
	 * @param value the value from the datastore
	 * @param toPojo the target pojo to load into
	 */
	public void loadSingleValue(String key, Object value, Object toPojo, LoadContext context)
	{
		Setter setter = this.rootSetters.get(key);
		if (setter != null)
		{
			setter.set(toPojo, value, context);
		}
		else
		{
			String mapPrefix = key;
			int lastDotIndex = mapPrefix.lastIndexOf('.');
			while (setter == null && lastDotIndex != -1)
			{
				mapPrefix = mapPrefix.substring(0, lastDotIndex);
				setter = this.rootSetters.get(mapPrefix);
				if (setter != null)
				{
					int mapKeyEnd = mapPrefix.length() + 1;
					int followingDot = key.indexOf('.', mapKeyEnd);
					if (followingDot == -1)
					{
						context.currentMapEntry = key.substring(mapKeyEnd);
						context.currentMapSuffix = "";
					}
					else
					{
						context.currentMapEntry = key.substring(mapKeyEnd, followingDot);
						context.currentMapSuffix = key.substring(followingDot + 1);
					}
					break;
				}
				lastDotIndex = mapPrefix.lastIndexOf('.');
			}
			if (setter != null)
			{
				setter.set(toPojo, value, context);
			}
		}
	}
	
	/**
	 * Saves the fields of a POJO into the properties of an Entity.  Does not affect id/parent
	 * (ie key) fields; those are assumed to already have been set.
	 * 
	 * @param fromPojo is your typed entity
	 * @param toEntity is a raw datastore entity
	 */
	public void save(T fromPojo, Entity toEntity)
	{
		// The default is to index all fields
		this.rootSaver.save(fromPojo, toEntity, Path.root(), false);
	}
}
