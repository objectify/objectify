package com.googlecode.objectify.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.Objectify;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.DatastoreUtils;

/**
 * <p>Transmogrifies POJO entities into datastore Entity objects and vice-versa.</p>
 * 
 * <p>TODO:  long explanation of how this works.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Transmog<T>
{
	/** The root translator that knows how to deal with an object of type T */
	Translator<T> rootTranslator;
	
	/** */
	KeyMetadata<T> keyMeta;
	
	/** */
	Set<Path> embedCollectionPoints;
	
	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, EntityMetadata<T> meta)
	{
		this.keyMeta = meta.getKeyMetadata();
		
		CreateContext ctx = new CreateContext(fact);
		this.rootTranslator = fact.getTranslators().createRoot(meta.getEntityClass(), ctx);
		this.embedCollectionPoints = ctx.getEmbedCollectionPoints();
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
			Node root = load(fromEntity);
			T pojo = load(root, new LoadContext(fromEntity, ofy));
			return pojo;
		}
		catch (LoadException ex) { throw ex; }
		catch (Exception ex) {
			throw new LoadException(fromEntity, ex.getMessage(), ex);
		}
	}
	
	/** Public just for testing */
	public T load(Node root, LoadContext ctx) throws LoadException {
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
			Node root = save(fromPojo, new SaveContext(ofy));
			Entity entity = save(root);
			return entity;
		}
		catch (SaveException ex) { throw ex; }
		catch (Exception ex) {
			throw new SaveException(fromPojo, ex.getMessage(), ex);
		}
	}
	
	/** Public just for testing */
	public Node save(T fromPojo, SaveContext ctx) {
		// Default index state is false!
		return (Node)rootTranslator.save(fromPojo, Path.root(), false, ctx);
	}

	/**
	 * <p>Turn the Entity into the hierarchical set of EntityNodes that the translation system understands.
	 * This is done in two steps.  The first converts to a literal structure of the Entity - this is normally
	 * the same as the POJO structure, but @Embed collections are "collectionized" such that the leaf property
	 * values are the collections.  The second step de-collectionizes the entity nodes, repositioning the
	 * collections from the leaf levels up to the place in the hierarchy where the embedded classes are.</p>
	 * 
	 * <p>Also adds id/parent fields to the root EntityNode.</p>
	 * 
	 * <p>Public only for testing purposes.</p>
	 * 
	 * <p>P.S. an exercise for the reader:  Try to do this in one pass!  Watch out for the ^null collection.</p>
	 * 
	 * @return a root EntityNode corresponding to the Entity, in a format suitable for translators.
	 */
	public Node load(Entity fromEntity) {
		Node root = this.loadLiterally(fromEntity);
		root = loadIntoTranslateFormat(root);

		// Last step, add the key fields to the root EntityNode so they get populated just like every other field would.
		String idName = keyMeta.getIdFieldName();
		
		if (root.containsKey(idName))
			throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Id field: " + fromEntity);
		
		if (fromEntity.getKey().isComplete()) {
			Object idValue = (fromEntity.getKey().getName() != null) ? fromEntity.getKey().getName() : fromEntity.getKey().getId();
			root.path(idName).setPropertyValue(idValue);
		}
		
		String parentName = keyMeta.getParentFieldName();
		if (parentName != null) {
			if (root.containsKey(parentName))
				throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Parent field: " + fromEntity);
			
			root.path(parentName).setPropertyValue(fromEntity.getKey().getParent());
		}
		
		return root;
	}
	
	/**
	 * <p>Break down the Entity into a series of nested EntityNodes which literally reflect
	 * the exact x.y.z paths of the properties in the Entity.</p>
	 * 
	 * <p>The resulting map will have @Embed collections as they are stored natively; another
	 * processing step will be required to create the normal hierarchical structure that translators
	 * can work with.</p>
	 * 
	 * @return a root EntityNode corresponding to a literal read of the Entity
	 */
	private Node loadLiterally(Entity fromEntity) {
		Node root = new Node(Path.root());
		
		for (Map.Entry<String, Object> prop: fromEntity.getProperties().entrySet()) {
			Path path = Path.of(prop.getKey());
			populateNode(root, path, prop.getValue());
		}
		
		return root;
	}
	
	/** 
	 * Recursive method that places the value at the path, building node structures along the way.
	 */
	private void populateNode(Node root, Path path, Object value) {
		Node bottom = createNesting(root, path.getPrevious());
		
		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> coll = (Collection<Object>)value;
			
			Node list = bottom.path(path.getSegment());
			for (Object obj: coll) {
				Node map = list.addToList();
				map.setPropertyValue(obj);
			}
		} else {
			Node map = bottom.path(path.getSegment());
			map.setPropertyValue(value);
		}
	}
	
	/** 
	 * Recursive method that builds out a nested linear chain of EntityNodes along the specified path.
	 * For example, if path is 'one.two.three', the nodes will be root:{one:{two:three{}}}.
	 * @return the bottom-most node in the list
	 */
	private Node createNesting(Node root, Path path) {
		if (path == Path.root()) {
			return root;
		} else {
			Node parent = createNesting(root, path.getPrevious());
			return parent.path(path.getSegment());
		}
	}

	/**
	 * <p>Converts a node tree from a direct interpretation of the Entity properties to the full hierarchical
	 * form suitable for Translators.  Modifies the graph as necessary; possibly even not at all.</p>
	 * 
	 * <p>Simple example:</p>
	 * <ul>
	 * <li>Example before: { things: { foo: [ "asdf" ], bar: [ 123 ] } }</li>
	 * <li>Exapmple after: { things: [ { foo:"asdf", bar:123 } ] }</li>
	 * </ul>
	 * 
	 * <p>More complicated example:</p>
	 * <ul>
	 * <li>Example before: { things: { foo: [ "asdf", "qwert" ], bar: [ 123, 456 ] } }</li>
	 * <li>Exapmple after: { things: [ { foo:"asdf", bar:123 }, { foo:"qwert", bar:456 } ] }</li>
	 * </ul>
	 * 
	 * <p>Keep in mind that there may be a hierarchy of embedded classes within the embedded collection.</p>
	 * 
	 * @param root is the root node of a literal interpretation of the Entity properties
	 * @return the same value passed in, but subgraphs may be changed to move @Embed collections to the proper place
	 */
	private Node loadIntoTranslateFormat(Node root) {
		return root;
	}
	
	/**
	 * <p>Turn a hierarchical series of EntityNodes into the standard Entity storage format.  Unlike the
	 * load process, this happens in one step, going straight to the "collectionized" format that @Embed
	 * collections are stored in.</p>
	 * 
	 * <p>Public only so we can test.</p>
	 * 
	 * @return an Entity with the propery key set
	 */
	public Entity save(Node root) {
		
		// Step one is extract the id/parent from root and create the Entity
		com.google.appengine.api.datastore.Key parent = null;
		if (keyMeta.hasParentField()) {
			Node parentNode = (Node)root.remove(keyMeta.getParentFieldName());
			parent = (com.google.appengine.api.datastore.Key)parentNode.getPropertyValue();
		}
		
		Node idNode = (Node)root.remove(keyMeta.getIdFieldName());
		Object id = idNode == null ? null : idNode.getPropertyValue();
		
		Entity ent = (id == null)
				? new Entity(keyMeta.getKind(), parent)
				: new Entity(DatastoreUtils.createKey(parent, keyMeta.getKind(), id));
		
		// Step two is populate the entity fields recursively
		populateFields(ent, root, false);
		
		return ent;
	}
	
	/**
	 * Recursively populate all the nodes onto the entity.
	 * 
	 * @param collectionize if true means that the value should be put in a collection property value at the end of the chain.
	 *  This goes to true whenever we hit an embedded collection.
	 */
	private void populateFields(Entity entity, Node node, boolean collectionize) {
		if (node.hasList()) {
			if (embedCollectionPoints.contains(node.getPath())) {
				// Watch for nulls to create the ^null collection
				List<Integer> nullIndexes = new ArrayList<Integer>();
				
				int index = 0;
				for (Node child: node) {
					if (child instanceof Node && ((Node)child).hasPropertyValue() && ((Node)child).getPropertyValue() == null)
						nullIndexes.add(index);
					else
						populateFields(entity, child, true);	// just switch to collectionizing
					
					index++;
				}
				
				if (!nullIndexes.isEmpty())
					setEntityProperty(entity, node.getPath().toPathString() + "^null", nullIndexes, false);
				
			} else {
				// A normal collection of leaf property values
				List<Object> things = new ArrayList<Object>(node.size());
				boolean index = false;	// everything in the list will have the same index state
				
				for (Node child: node) {
					Node map = (Node)child;
					if (!map.hasPropertyValue())
						map.getPath().throwIllegalState("Expected property value, got " + map);
					
					things.add(map.getPropertyValue());
					index = map.isPropertyIndexed();
				}
				
				setEntityProperty(entity, node.getPath().toPathString(), things, index);
			}
			
		} else {	// EntityNode
			if (node.hasPropertyValue()) {
				String propertyName = node.getPath().toPathString();
				if (collectionize) {
					@SuppressWarnings("unchecked")
					List<Object> list = (List<Object>)entity.getProperty(propertyName);
					if (list == null) {
						list = new ArrayList<Object>();
						setEntityProperty(entity, propertyName, list, node.isPropertyIndexed());
					}
					
					list.add(node.getPropertyValue());
				} else {
					setEntityProperty(entity, propertyName, node.getPropertyValue(), node.isPropertyIndexed());
				}
			}
					
			if (!node.isEmpty()) {
				for (Node child: node) {
					populateFields(entity, child, collectionize);
				}
			}
		}
	}
	
	/** Utility method */
	private void setEntityProperty(Entity entity, String propertyName, Object value, boolean index) {
		if (index)
			entity.setProperty(propertyName, value);
		else
			entity.setUnindexedProperty(propertyName, value);
	}
}
