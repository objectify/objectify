package com.googlecode.objectify.impl.load;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import com.googlecode.objectify.impl.FieldWrapper;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.TypeUtils;

/**
 * <p>This is a base class for handling setter operations on collections and arrays.</p>
 */
abstract public class EmbeddedMultivalueSetter extends CollisionDetectingSetter
{
	/**
	 * The field which holds the embedded collection. We use FieldWrapper instead of
	 * Field because we want to use methods that take a the wrapper type.
	 */
	FieldWrapper field;
	
	/**
	 * The blah.blah.blah path to the embedded collection.  This is used as
	 * a base path to discover the null values index.
	 */
	String path;
	
	/** */
	public EmbeddedMultivalueSetter(Field field, String path, Collection<String> collisionPaths)
	{
		super(collisionPaths);
		
		assert TypeUtils.isEmbedded(field);
		assert TypeUtils.isArrayOrCollection(field.getType());
		
		this.field = new FieldWrapper(field);
		this.path = path;
	}
	
	/** @return the no-arg constructor of the embedded type */
	protected abstract Constructor<?> getComponentConstructor();
	
	/**
	 * Gets the collection in the relevant field of the specified POJO, or creates (and
	 * sets) a new one.  If the field is an array type, set it up and return a Collection
	 * facade of the array.
	 * 
	 * @param toPojo is the entity pojo that has a field for us to set
	 * @param size is the size of the pojo to create, if necessary
	 */
	protected abstract Collection<Object> getOrCreateCollection(Object toPojo, int size);
	
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.CollisionDetectingSetter#safeSet(java.lang.Object, java.lang.Object, com.googlecode.objectify.impl.LoadContext)
	 */
	@Override
	@SuppressWarnings("unchecked")
	protected void safeSet(final Object toPojo, Object value, final LoadContext context)
	{
		// Let's just ignore nulls in the dataset and leave the collections as default.  We don't save them so we might
		// as well not load them.  Maybe at some future date we will change this approach and explicitly set/get null
		// collections.
		if (value == null)
			return;
			
		// The datastore always gives us collections, never a native array
		if (!(value instanceof Collection<?>))
			throw new IllegalStateException("Tried to load a non-collection type into embedded collection " + this.field);

		Collection<Object> datastoreCollection = (Collection<Object>)value;
		
		// We will need the null state set, which might be null itself
		Set<Integer> nullIndexes = TypeUtils.getNullIndexes(context.getEntity(), this.path);
		
		// Some nulls, some real, this is what we get
		final int collectionSize = (nullIndexes == null)
			? datastoreCollection.size()
			: datastoreCollection.size() + nullIndexes.size();	

		final ArrayList<Object> pending = context.getPendingEmbeddedMultivalue(this.path, collectionSize);
		if (pending.isEmpty())
		{
			// Initialize it with relevant POJOs
			for (int i=0; i<collectionSize; i++)
			{
				// Make an explicit null check instead of using emptySet() to reduce autoboxing overhead
				if (nullIndexes != null && nullIndexes.contains(i))
				{
					pending.add(null);
				}
				else
				{
					Object embedded = TypeUtils.newInstance(this.getComponentConstructor());
					pending.add(embedded);
				}
			}
			
			// The very last step is to add all the pending objects into the actual collection on the POJO.
			// This is safe now because all objects are fully populated and hashable.
			context.addDoneHandler(new Runnable() {
				@Override
				public void run()
				{
					Collection<Object> embeddedMultivalue = getOrCreateCollection(toPojo, collectionSize);
					embeddedMultivalue.addAll(pending);
				}
			});
		}
		
		// There will be a datastore value for each of the non-null POJOs in the collection
		Iterator<Object> datastoreIterator = datastoreCollection.iterator();
		for (Object embedded: pending)
		{
			if (embedded != null)
			{
				Object datastoreValue = datastoreIterator.next();
				this.next.set(embedded, datastoreValue, context);
			}
		}
	}
}
