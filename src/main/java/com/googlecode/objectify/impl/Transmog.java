package com.googlecode.objectify.impl;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.EntityClassTranslator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.util.DatastoreUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Transmogrifies POJO entities into datastore Entity objects and vice-versa.</p>
 *
 * <p>TODO:  long explanation of how this works.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class Transmog<T>
{
	/** */
	private static final Logger log = Logger.getLogger(Transmog.class.getName());

	/** The root translator that knows how to deal with an object of type T */
	EntityClassTranslator<T> rootTranslator;

	/** */
	Set<Path> embedCollectionPoints;
	
	/** */
	Set<Path> leaveEmbeddedEntityAlonePoints;
	Set<Path> leaveEmbeddedEntityAloneParentPoints;

	/** */
	ObjectifyFactory fact;

	/**
	 * Creats a transmog for the specified class, introspecting it and discovering
	 * how to load/save its properties.
	 */
	public Transmog(ObjectifyFactory fact, Class<T> clazz)
	{
		this.fact = fact;
		
		CreateContext ctx = new CreateContext(fact);
		this.rootTranslator = new EntityClassTranslator<T>(clazz, ctx);
		this.embedCollectionPoints = ctx.getEmbedCollectionPoints();
		this.leaveEmbeddedEntityAlonePoints = ctx.getLeaveEmbeddedEntityAlonePoints();
		this.leaveEmbeddedEntityAloneParentPoints = ctx.getLeaveEmbeddedEntityAloneParentPoints();
	}

	/** */
	public KeyMetadata<T> getKeyMetadata() {
		return this.rootTranslator;
	}

	/**
	 * Create a pojo from the Entity.
	 *
	 * @param fromEntity is a raw datastore entity
	 * @return the entity converted into the relevant pojo
	 */
	public T load(Entity fromEntity, LoadContext ctx) throws LoadException
	{
		try {
			// The context needs to know the root entity for any given point
			ctx.setCurrentRoot(Key.create(fromEntity.getKey()));

			Node root = load(fromEntity);
			T pojo = load(root, ctx);
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
	public Entity save(T fromPojo, SaveContext ctx)
	{
		if (log.isLoggable(Level.FINEST))
			log.finest("\tTranslating " + fromPojo);

		try {
			// The context needs to know the root entity for any given point
			ctx.setCurrentRoot(fromPojo);

			Node root = saveToNode(fromPojo, ctx);
			Entity entity = save(root);
			
			createSyntheticIndexes(entity, ctx);
			
			return entity;
		}
		catch (SaveException ex) { throw ex; }
		catch (Exception ex) {
			throw new SaveException(fromPojo, ex.getMessage(), ex);
		}
	}

	/**
	 * If we are in v2 mode, this will establish any synthetic dot-separate indexes
	 * for embedded things that are indexed.
	 */
	private void createSyntheticIndexes(Entity entity, SaveContext ctx) {
		
		// Only do this for v2
		if (!fact.getSaveWithNewEmbedFormat())
			return;

		// Look for anything with depth more than 1; these are embedded things
		for (Map.Entry<Path, Collection<Object>> index: ctx.getIndexes().entrySet()) {
			Path path = index.getKey();
			Collection<Object> values = index.getValue();
			
			if (path.isEmbedded()) {
				entity.setProperty(path.toPathString(), values);
			}
		}
	}

	/** Public just for testing */
	public Node saveToNode(T fromPojo, SaveContext ctx) {
		// Default index state is false!
		return (Node)rootTranslator.save(fromPojo, Path.root(), false, ctx);
	}

	/**
	 * <p>Turn the Entity into the hierarchical set of Nodes that the translation system understands.
	 * This is done in two steps.  The first converts to a literal structure of the Entity - this is normally
	 * the same as the POJO structure, but @Embed collections are "collectionized" such that the leaf property
	 * values are the collections.  The second step de-collectionizes the entity nodes, repositioning the
	 * collections from the leaf levels up to the place in the hierarchy where the embedded classes are.</p>
	 *
	 * <p>Also adds id/parent fields to the root Node.</p>
	 *
	 * <p>Public only for testing purposes.</p>
	 *
	 * <p>P.S. an exercise for the reader:  Try to do this in one pass!  Watch out for the ^null collection.</p>
	 *
	 * @return a root Node corresponding to the Entity, in a format suitable for translators.
	 */
	public Node load(Entity fromEntity) {

		if (log.isLoggable(Level.FINEST))
			log.finest("\tTranslating " + fromEntity);

		Node root = this.loadLiterally(fromEntity);

		// No embed collections?  No changes necessary, we can optimize out the graph walk
		if (!this.embedCollectionPoints.isEmpty() && needsModificationIntoTranslateFormat(fromEntity))
			this.modifyIntoTranslateFormat(root);

		// Last step, add the key fields to the root Node so they get populated just like every other field would.
		String idName = getKeyMetadata().getIdFieldName();

		if (root.containsKey(idName))
			throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Id field: " + fromEntity);

		if (fromEntity.getKey().isComplete()) {
			Object idValue = (fromEntity.getKey().getName() != null) ? fromEntity.getKey().getName() : fromEntity.getKey().getId();
			root.path(idName).setPropertyValue(idValue);
		}

		String parentName = getKeyMetadata().getParentFieldName();
		if (parentName != null) {
			if (root.containsKey(parentName))
				throw new IllegalStateException("Datastore Entity has a property whose name overlaps with the @Parent field: " + fromEntity);

			root.path(parentName).setPropertyValue(fromEntity.getKey().getParent());
		}

		return root;
	}
	
	/**
	 * v1 embed format needs modification. We determine that the entity is in the v2 format
	 * if there are any EmbeddedEntity objects at the collection embed points.
	 * 
	 * @return true if the entity has the v1 embedded format
	 */
	private boolean needsModificationIntoTranslateFormat(Entity fromEntity) {
		for (Path path: embedCollectionPoints) {
			Object value = fromEntity.getProperty(path.toPathString());
			if (value != null && value instanceof Collection) {
				return false;
			}
		}

		return true;
	}
	
	/**
	 * <p>Break down the Entity into a series of nested Nodes which literally reflect
	 * the exact x.y.z paths of the properties in the Entity.</p>
	 *
	 * <p>The resulting map will have @Embed collections as they are stored natively; another
	 * processing step will be required to create the normal hierarchical structure that translators
	 * can work with.</p>
	 *
	 * @return a root Node corresponding to a literal read of the Entity
	 */
	private Node loadLiterally(Entity fromEntity) {
		Node root = new Node(Path.root());

		for (Map.Entry<String, Object> prop: fromEntity.getProperties().entrySet()) {
			Path path = Path.of(prop.getKey());

			// If we have a new format entity, don't try to populate the synthetic index
			if (path.isEmbedded() && fromEntity.hasProperty(path.getPrevious().toPathString()))
				continue;

			populateNode(root, path, prop.getValue());
		}

		return root;
	}

	/**
	 * Recursive method that places the value at the path, building node structures along the way.
	 */
	private void populateNode(Node root, Path path, Object value) {
		// Build basic nodes up to last level
		Node bottom = createNesting(root, path.getPrevious());

		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> coll = (Collection<Object>)value;

			Node list = bottom.path(path.getSegment());
			for (Object obj: coll) {
				Node content = recursivelyBuildNode(list.getPath(), obj);
				list.addToList(content);
			}
		} else if (value instanceof EmbeddedEntity && !shouldLeaveEmbeddedEntityAlone(path)) {
			for (Map.Entry<String, Object> entry: ((EmbeddedEntity)value).getProperties().entrySet()) {
				populateNode(root, path.extend(entry.getKey()), entry.getValue());
			}
				
		} else {
			Node map = bottom.path(path.getSegment());
			map.setPropertyValue(value);
		}
	}
	
	/**
	 * This is a hacked-in alternative way of constructing nodes which we use when we
	 * hit a List or EmbeddedEntity structure. We know that there will be no more dot-separated
	 * components at this point, so we can use a more traditional recursive construction process.
	 * This is a mess that will go away when we get rid of the Node system.
	 */
	private Node recursivelyBuildNode(Path path, Object value) {
		if (value instanceof Collection) {
			@SuppressWarnings("unchecked")
			Collection<Object> coll = (Collection<Object>)value;

			Node list = new Node(path);
			
			for (Object obj: coll) {
				Node content = recursivelyBuildNode(list.getPath(), obj);
				list.addToList(content);
			}
			
			return list;
			
		} else if (value instanceof EmbeddedEntity && !shouldLeaveEmbeddedEntityAlone(path)) {
			Node map = new Node(path);
			
			for (Map.Entry<String, Object> entry: ((EmbeddedEntity)value).getProperties().entrySet()) {
				Node prop = recursivelyBuildNode(path.extend(entry.getKey()), entry.getValue());
				map.addToMap(prop);
			}
			
			return map;
				
		} else {
			Node thing = new Node(path);
			thing.setPropertyValue(value);
			return thing;
		}
	}

	/**
	 * @return true if EmbeddedEntity should be left unmunged into nodes. This will be the case
	 * if the field it gets assigned to is of type EmbeddedEntity or Object.
	 */
	private boolean shouldLeaveEmbeddedEntityAlone(Path path) {
		return leaveEmbeddedEntityAlonePoints.contains(path) || leaveEmbeddedEntityAloneParentPoints.contains(path.getPrevious());
	}

	/**
	 * Recursive method that builds out a nested linear chain of Nodes along the specified path.
	 * For example, if path is 'one.two.three', the nodes will be root:{one:{two:three:{}}}.
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
	 * This is used in the implementation of modifyIntoTranslateFormat().  The exception is instantiated once
	 * as has no stacktrace so it should be about as efficient as a normal return sequence.
	 */
	@SuppressWarnings("serial")
	private static class StopException extends Exception {
		public static final StopException INSTANCE = new StopException();
		private StopException() {}
		@Override public synchronized Throwable fillInStackTrace() { return this; }
	}

	/**
	 * <p>Recursively converts a node tree from a direct interpretation of the Entity properties to the full hierarchical
	 * form suitable for Translators.  Modifies the graph as necessary; possibly even not at all (ie, no embed collections).</p>
	 *
	 * <p>Simple example of an embed collection at 'things':</p>
	 * <ul>
	 * <li>Before: { things: { foo: [ "asdf" ], bar: [ 123 ] } }</li>
	 * <li>After:  { things: [ { foo:"asdf", bar:123 } ] }</li>
	 * </ul>
	 *
	 * <p>If there were multiple values it would look like this:</p>
	 * <ul>
	 * <li>Before: { things: { foo: [ "asdf", "qwert" ], bar: [ 123, 456 ] } }</li>
	 * <li>After:  { things: [ { foo:"asdf", bar:123 }, { foo:"qwert", bar:456 } ] }</li>
	 * </ul>
	 *
	 * <p>Keep in mind that there may be a hierarchy of embedded classes within the embedded collection.</p>
	 *
	 * @param thingsBefore is a subnode of a literal interpretation of the Entity properties; it will be changed
	 *  into a format that Translators understand.
	 */
	private void modifyIntoTranslateFormat(Node thingsBefore) {
		if (embedCollectionPoints.contains(thingsBefore.getPath())) {

			Node nullsNode = thingsBefore.remove(Path.NULL_INDEXES);	// take this out of the data model
			Set<Integer> nulls = getNullIndexes(nullsNode);

			// We must de-collectionize the subgraph
			List<Node> thingsAfter = new ArrayList<Node>();

			int beforeIndex = 0;
			int afterIndex = 0;
			while (true) {
				if (!nulls.isEmpty() && nulls.remove(afterIndex)) {
					// Add a null to the collection
					Node node = new Node(thingsBefore.getPath());
					node.setPropertyValue(null);
					thingsAfter.add(node);
					afterIndex++;

				} else if (!thingsBefore.isEmpty()) {
					// Add a real value to the collection
					try {
						Node atIndexAfter = new Node(thingsBefore.getPath());

						for (Node fooBefore: thingsBefore)
							copyNode(beforeIndex, fooBefore, atIndexAfter);

						thingsAfter.add(atIndexAfter);

						beforeIndex++;
						afterIndex++;
					}
					catch (StopException ex) {
						break;
					}

				} else {
					break;
				}
			}

			thingsBefore.setList(thingsAfter);
		}
		else if (thingsBefore.hasMap()) {	// Recurse until we find a place we need to change... but only need to recurse on map nodes
			for (Node child: thingsBefore)
				modifyIntoTranslateFormat(child);
		}
	}

	/**
	 * Decollectionizes one value.
	 *
	 * @param collectionIndex is the index in the final collection to extract
	 * @param from is the node subtree to copy into "to"; it's "foo" in the javadoc example of modifyIntoTranslateFormat()
	 * @param to is the container of the new node subtree representing from
	 * @throws StopException if the index goes past the end
	 */
	private void copyNode(int collectionIndex, Node from, Node to) throws StopException {
		if (from.hasList()) {
			if (collectionIndex >= from.size())
				throw StopException.INSTANCE;

			Node choice = from.get(collectionIndex);
			to.addToMap(choice);
		} else {
			for (Node fromChild: from) {
				copyNode(collectionIndex, fromChild, to.path(from.getPath().getSegment()));
			}
		}
	}

	/**
	 * Construct a Set<Integer> from the '^null' collection of an embedded collection.
	 * @return an empty set if nullsNode is null
	 */
	private Set<Integer> getNullIndexes(Node nullsNode) {
		if (nullsNode == null)
			return Collections.emptySet();

		Set<Integer> nulls = new HashSet<Integer>(nullsNode.size() * 2);
		for (Node child: nullsNode)
			nulls.add(((Number)child.getPropertyValue()).intValue());

		return nulls;
	}

	/**
	 * <p>Turn a hierarchical series of Nodes into the standard Entity storage format.  Unlike the
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
		if (getKeyMetadata().hasParentField()) {
			Node parentNode = (Node)root.remove(getKeyMetadata().getParentFieldName());
			parent = (com.google.appengine.api.datastore.Key)parentNode.getPropertyValue();
		}

		Node idNode = (Node)root.remove(getKeyMetadata().getIdFieldName());
		Object id = idNode == null ? null : idNode.getPropertyValue();
		
		if (id == null && !getKeyMetadata().isIdGeneratable())
			throw new IllegalStateException(getKeyMetadata().getIdFieldType().getSimpleName() + " @Id fields cannot be null. Only Long @Id fields can be autogenerated.");

		Entity ent = (id == null)
				? new Entity(getKeyMetadata().getKind(), parent)
				: new Entity(DatastoreUtils.createKey(parent, getKeyMetadata().getKind(), id));

		// Step two is populate the entity fields recursively
		if (fact.getSaveWithNewEmbedFormat()) {
			ent.setPropertiesFrom((PropertyContainer)convertToNewFormat(root));
		} else {
			populateFieldsOldFormat(ent, root, false);
		}

		return ent;
	}

	/**
	 * Create the datastore-level structure that corresponds to the Node. This will recurse through the
	 * node structure; the value returned will either be a List, PropertyContainer, or some sort of native storage type.
	 * This is the new format for embedded structures.
	 */
	private Object convertToNewFormat(Node node) throws SkipException {
		if (node.hasMap()) {
			EmbeddedEntity emb = new EmbeddedEntity();
			
			for (Node child: node) {
				try {
					Object value = convertToNewFormat(child);
					setEntityProperty(emb, child.getPath().getSegment(), value, child.isPropertyIndexed());
				} catch (SkipException ex) {}
			}
			
			return emb;
			
		} else if (node.hasList()) {
			// A normal collection of leaf property values
			List<Object> things = new ArrayList<Object>(node.size());

			for (Node child: node) {
				try {
					things.add(convertToNewFormat(child));
				} catch (SkipException ex) {}
			}

			return things;
			
		} else if (node.hasPropertyValue()) {
			return node.getPropertyValue();
			
		} else {
			// Happens when we have empty embedded collections. In which case we should do nothing.
			throw new SkipException();
		}
	}
	
	/**
	 * Recursively populate all the nodes onto the entity.
	 *
	 * @param collectionize if true means that the value should be put in a collection property value at the end of the chain.
	 *  This goes to true whenever we hit an embedded collection.
	 */
	private void populateFieldsOldFormat(Entity entity, Node node, boolean collectionize) {
		if (node.hasList()) {
			if (embedCollectionPoints.contains(node.getPath())) {
				// Watch for nulls to create the ^null collection
				List<Integer> nullIndexes = new ArrayList<Integer>();

				int index = 0;
				for (Node child: node) {
					if (child instanceof Node && ((Node)child).hasPropertyValue() && ((Node)child).getPropertyValue() == null)
						nullIndexes.add(index);
					else
						populateFieldsOldFormat(entity, child, true);	// just switch to collectionizing

					index++;
				}

				if (!nullIndexes.isEmpty())
					setEntityProperty(entity, node.getPath().extend(Path.NULL_INDEXES).toPathString(), nullIndexes, false);

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

		} else {	// Not list
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

			if (node.hasMap()) {
				for (Node child: node) {
					populateFieldsOldFormat(entity, child, collectionize);
				}
			}
		}
	}

	/** Utility method */
	private void setEntityProperty(PropertyContainer entity, String propertyName, Object value, boolean index) {
		if (index)
			entity.setProperty(propertyName, value);
		else
			entity.setUnindexedProperty(propertyName, value);
	}
}
