package com.googlecode.objectify.impl;

import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.load.ClassLoadr;
import com.googlecode.objectify.impl.load.EntityNode;
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
	ClassLoadr<T> rootLoader;
	
	/** */
	KeyMetadata<T> keyMeta;
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, EntityMetadata<T> meta)
	{
		this.keyMeta = meta.getKeyMetadata();
		this.rootLoader = new ClassLoadr<T>(fact, meta.getEntityClass());
		this.rootSaver = new RootClassSaver(fact, clazz);
	}
	
	/**
	 * Create a pojo from the Entity.
	 * 
	 * @param fromEntity is a raw datastore entity
	 * @return the entity converted into the relevant pojo
	 */
	public T load(Entity fromEntity, Objectify ofy)
	{
		EntityNode root = createEntityNode(fromEntity);
		LoadContext context = new LoadContext(fromEntity, ofy);
		
		T pojo = rootLoader.load(root, context);
		
		context.done();
		
		return pojo;
	}
	
	/**
	 * Break down the Entity into a series of nested EntityNode (HashMap) objects
	 * which reflect the x.y paths.  The root EntityNode will also contain the key
	 * fields and values.
	 * 
	 * @return a root EntityNode corresponding to the Entity
	 */
	private EntityNode createEntityNode(Entity fromEntity) {
		EntityNode root = new EntityNode(Path.root());
		
		for (Map.Entry<String, Object> prop: fromEntity.getProperties().entrySet()) {
			EntityNode here = root;
			String[] parts = prop.getKey().split("\\.");
			Path path = Path.root();
			
			for (String part: parts) {
				path = path.extend(part);
				EntityNode node = (EntityNode)here.get(part);
				if (node == null) {
					node = new EntityNode(path);
					here.put(part, node);
				}
				
				here = node;
			}
			
			here.setPropertyValue(prop.getValue());
		}
		
		// Last step, add the key fields to the root EntityNode so they get populated just like every other field would.
		String idName = keyMeta.getIdFieldName();
		
		if (root.containsKey(idName))
			throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Id field: " + fromEntity);
		
		if (fromEntity.getKey().getName() != null)
			root.put(idName, fromEntity.getKey().getName());
		else
			root.put(idName, fromEntity.getKey().getId());
		
		String parentName = keyMeta.getParentFieldName();
		if (parentName != null) {
			if (root.containsKey(parentName))
				throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Parent field: " + fromEntity);
			
			root.put(parentName, fromEntity.getKey().getParent());
		}
		
		return root;
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
