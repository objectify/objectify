package com.googlecode.objectify.impl;

import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.load.Loader;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.impl.node.MapNode;
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
	Loader<T> rootLoader;
	
	/** */
	KeyMetadata<T> keyMeta;
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, EntityMetadata<T> meta)
	{
		this.keyMeta = meta.getKeyMetadata();
		this.rootLoader = fact.getLoaders().createRoot(meta.getEntityClass());
		//this.rootSaver = new RootClassSaver(fact, clazz);
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
		
		return pojo;
	}
	
	/**
	 * Break down the Entity into a series of nested EntityNodes
	 * which reflect the x.y.z paths.  The root EntityNode will also contain the key
	 * fields and values.
	 * 
	 * @return a root EntityNode corresponding to the Entity
	 */
	private MapNode createEntityNode(Entity fromEntity) {
		MapNode root = new MapNode(Path.root());
		
		for (Map.Entry<String, Object> prop: fromEntity.getProperties().entrySet()) {
			MapNode here = root;
			String[] parts = prop.getKey().split("\\.");
			
			int end = parts.length - 1;
			for (int i=0; i<end; i++) {
				String part = parts[i];
				here = here.pathMap(part);
			}
			
			// The last one gets handled specially, it might need to be a ListNode
			String part = parts[end];
			
			if (prop.getValue() instanceof Collection) {
				@SuppressWarnings("unchecked")
				Collection<Object> coll = (Collection<Object>)prop.getValue();
				
				ListNode list = here.pathList(part);
				for (Object obj: coll) {
					MapNode map = list.add();
					map.setPropertyValue(obj);
				}
			} else {
				here = here.pathMap(part);
				here.setPropertyValue(prop.getValue());
			}
		}
		
		// Last step, add the key fields to the root EntityNode so they get populated just like every other field would.
		String idName = keyMeta.getIdFieldName();
		
		if (root.containsKey(idName))
			throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Id field: " + fromEntity);
		
		Object idValue = (fromEntity.getKey().getName() != null) ? fromEntity.getKey().getName() : fromEntity.getKey().getId();
		root.pathMap(idName).setPropertyValue(idValue);
		
		String parentName = keyMeta.getParentFieldName();
		if (parentName != null) {
			if (root.containsKey(parentName))
				throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Parent field: " + fromEntity);
			
			root.pathMap(parentName).setPropertyValue(fromEntity.getKey().getParent());
		}
		
		return root;
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
