package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which can load things into a collection field.  Might be embedded items, might not.</p>
 * 
 * <p>Note that empty or null collections are not stored in the datastore, and null values for the collection
 * field are ignored when they are loaded from the Entity.  This is because the datastore doesn't store empty
 * collections, and storing null fields will confuse filtering for actual nulls in the collection contents.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTranslatorFactory implements TranslatorFactory<Collection<Object>>
{
	abstract public static class CollectionListNodeTranslator<T> extends ListNodeTranslator<Collection<T>> {
		
		/** Same as having a null existing collection */
		@Override
		final protected Collection<T> loadList(ListNode node, LoadContext ctx) {
			return loadListIntoExistingCollection(node, ctx, null);
		}
		
		/**
		 * Load into an existing collection; allows us to recycle collection instances on entities, which might have
		 * exotic concrete types or special initializers (comparators, etc).
		 * 
		 * @param coll can be null to trigger creating a new collection 
		 */
		abstract public Collection<T> loadListIntoExistingCollection(ListNode node, LoadContext ctx, Collection<T> coll);
	}
	
	@Override
	public Translator<Collection<Object>> create(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx) {
		@SuppressWarnings("unchecked")
		final Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>)GenericTypeReflector.erase(type);
		
		if (!Collection.class.isAssignableFrom(collectionType))
			return null;
		
		ctx.setInCollection(true);
		try {
			final ObjectifyFactory fact = ctx.getFactory();
			
			Type componentType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
			if (componentType == null)	// if it was a raw type, just assume Object
				componentType = Object.class;
			
			final Translator<Object> componentTranslator = fact.getTranslators().create(path, fieldAnnotations, componentType, ctx);
			
			return new CollectionListNodeTranslator<Object>() {
				@Override
				@SuppressWarnings("unchecked")
				public Collection<Object> loadListIntoExistingCollection(ListNode node, LoadContext ctx, Collection<Object> collection) {
					if (collection == null)
						collection = (Collection<Object>)fact.constructCollection(collectionType, node.size());
					else
						collection.clear();
					
					for (EntityNode child: node) {
						try {
							Object value = componentTranslator.load(child, ctx);
							collection.add(value);
						}
						catch (SkipException ex) {
							// No prob, just skip that one
						}
					}
	
					return collection;
				}
	
				@Override
				protected ListNode saveList(Collection<Object> pojo, Path path, boolean index, SaveContext ctx) {
					
					// If the collection is null, just skip it.  This is important because of the way filtering works;
					// if we stored a null then the field would match when filtering for null (same as a null in the list).
					// Also, storing a null would forcibly assign null to the collection field on load, screwing things up
					// if the developer decided to later initialize the collection in the default constructor.
					if (pojo == null)
						throw new SkipException();
					
					// If it's empty, might as well skip it - the datastore doesn't store empty lists
					if (pojo.isEmpty())
						throw new SkipException();
					
					ListNode node = new ListNode(path);

					if (pojo != null) {
						for (Object obj: pojo) {
							try {
								EntityNode child = componentTranslator.save(obj, path, index, ctx);
								node.add(child);
							}
							catch (SkipException ex) {
								// Just skip that node, no prob
							}
						}
					}
					
					return node;
				}
			};
		}
		finally {
			ctx.setInCollection(false);
		}
	}
}
