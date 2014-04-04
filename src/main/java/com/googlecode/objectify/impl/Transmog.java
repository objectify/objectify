package com.googlecode.objectify.impl;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.LoadException;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.SaveException;
import com.googlecode.objectify.impl.translate.CreateContext;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.SaveContext;
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
	/** */
	private static final Logger log = Logger.getLogger(Transmog.class.getName());

	/** The root translator that knows how to deal with an object of type T */
	EntityClassTranslator<T> rootTranslator;

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

			Entity entity = (Entity)rootTranslator.save(fromPojo, false, ctx, Path.root());

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
		
		// Look for anything which is embedded and therefore won't be automatically indexed
		for (Map.Entry<Path, Collection<Object>> index: ctx.getIndexes().entrySet()) {
			Path path = index.getKey();
			Collection<Object> values = index.getValue();
			
			if (path.isEmbedded()) {
				entity.setProperty(path.toPathString(), values);
			}
		}
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
		ent.setPropertiesFrom((PropertyContainer)convertToNewFormat(root));

		return ent;
	}
	
	/** Utility method */
	private void setEntityProperty(PropertyContainer entity, String propertyName, Object value, boolean index) {
		if (index)
			entity.setProperty(propertyName, value);
		else
			entity.setUnindexedProperty(propertyName, value);
	}
}
