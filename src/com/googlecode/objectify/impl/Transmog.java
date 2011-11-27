package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.impl.node.MapNode;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.DatastoreUtils;

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
	/** The root translator that knows how to deal with an object of type T */
	Translator<T> rootTranslator;
	
	/** */
	KeyMetadata<T> keyMeta;
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, EntityMetadata<T> meta)
	{
		this.keyMeta = meta.getKeyMetadata();
		this.rootTranslator = fact.getTranslators().createRoot(meta.getEntityClass());
	}
	
	/**
	 * Create a pojo from the Entity.
	 * 
	 * @param fromEntity is a raw datastore entity
	 * @return the entity converted into the relevant pojo
	 */
	public T load(Entity fromEntity, Objectify ofy) throws LoadException
	{
		try {
			EntityNode root = createNode(fromEntity);
			T pojo = load(root, new LoadContext(fromEntity, ofy));
			return pojo;
		}
		catch (LoadException ex) { throw ex; }
		catch (Exception ex) {
			throw new LoadException(fromEntity, ex.getMessage(), ex);
		}
	}
	
	/** Public just for testing */
	public T load(EntityNode root, LoadContext ctx) throws LoadException {
		return rootTranslator.load(root, ctx);
	}
	
	/**
	 * Creates an Entity that has been set up with the content of the pojo, including key fields.
	 * 
	 * @param fromPojo is your typed entity
	 */
	public Entity save(T fromPojo, Objectify ofy)
	{
		try {
			MapNode root = save(fromPojo, new SaveContext(ofy));
			Entity entity = createEntity(root);
			return entity;
		}
		catch (SaveException ex) { throw ex; }
		catch (Exception ex) {
			throw new SaveException(fromPojo, ex.getMessage(), ex);
		}
	}
	
	/** Public just for testing */
	public MapNode save(T fromPojo, SaveContext ctx) {
		// Default index state is false!
		return (MapNode)rootTranslator.save(fromPojo, false, ctx);
	}

	/**
	 * Break down the Entity into a series of nested EntityNodes
	 * which reflect the x.y.z paths.  The root EntityNode will also contain the key
	 * fields and values.  Public only so we can test.
	 * 
	 * @return a root EntityNode corresponding to the Entity
	 */
	public MapNode createNode(Entity fromEntity) {
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
	 * Reconstitute an Entity from a broken down series of nested EntityNodes.
	 * Public only so we can test.
	 * 
	 * @return an Entity with the propery key set
	 */
	public Entity createEntity(MapNode root) {
		
		// Step one is extract the id/parent from root and create the Entity
		com.google.appengine.api.datastore.Key parent = null;
		if (keyMeta.hasParentField()) {
			MapNode parentNode = (MapNode)root.remove(keyMeta.getParentFieldName());
			parent = (com.google.appengine.api.datastore.Key)parentNode.getPropertyValue();
		}
		
		MapNode idNode = (MapNode)root.remove(keyMeta.getIdFieldName());
		Object id = idNode == null ? null : idNode.getPropertyValue();
		
		Entity ent = (id == null)
				? new Entity(keyMeta.getKind(), parent)
				: new Entity(DatastoreUtils.createKey(parent, keyMeta.getKind(), id));
		
		// Step two is populate the entity fields recursively
		populateFields(ent, root);
		
		return ent;
	}
	
	/**
	 * Recursively populate all the nodes onto the entity
	 */
	private void populateFields(Entity entity, EntityNode node) {
		if (node instanceof ListNode) {
			ListNode listNode = (ListNode)node;
			List<Object> things = new ArrayList<Object>(listNode.size());
			boolean index = false;
			
			for (EntityNode child: listNode) {
				MapNode map = (MapNode)child;
				if (!map.hasPropertyValue())
					map.getPath().throwIllegalState("Expected property value, got " + map);
				
				things.add(map.getPropertyValue());
				index = map.isPropertyIndexed();
			}
			
			if (index)
				entity.setProperty(listNode.getPath().toPathString(), things);
			else
				entity.setUnindexedProperty(listNode.getPath().toPathString(), things);
			
		} else {	// MapNode
			MapNode mapNode = (MapNode)node;
			
			if (mapNode.hasPropertyValue())
				if (mapNode.isPropertyIndexed())
					entity.setProperty(mapNode.getPath().toPathString(), mapNode.getPropertyValue());
				else
					entity.setUnindexedProperty(mapNode.getPath().toPathString(), mapNode.getPropertyValue());
					
			if (!mapNode.isEmpty()) {
				for (EntityNode child: mapNode.values()) {
					populateFields(entity, child);
				}
			}
		}
	}
}
