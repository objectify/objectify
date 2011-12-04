package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
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
	abstract public static class CollectionListNodeTranslator extends ListNodeTranslator<Collection<Object>> {
		
		/** Same as having a null existing collection */
		@Override
		final protected Collection<Object> loadList(Node node, LoadContext ctx) {
			return loadListIntoExistingCollection(node, ctx, null);
		}
		
		/**
		 * Load into an existing collection; allows us to recycle collection instances on entities, which might have
		 * exotic concrete types or special initializers (comparators, etc).
		 * 
		 * @param coll can be null to trigger creating a new collection 
		 */
		abstract public Collection<Object> loadListIntoExistingCollection(Node node, LoadContext ctx, Collection<Object> coll);
	}
	
	@Override
	public Translator<Collection<Object>> create(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx) {
		@SuppressWarnings("unchecked")
		final Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>)GenericTypeReflector.erase(type);
		
		if (!Collection.class.isAssignableFrom(collectionType))
			return null;
		
		ctx.enterCollection(path);
		try {
			final ObjectifyFactory fact = ctx.getFactory();
			
			Type componentType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
			if (componentType == null)	// if it was a raw type, just assume Object
				componentType = Object.class;
			
			final Translator<Object> componentTranslator = fact.getTranslators().create(path, fieldAnnotations, componentType, ctx);
			
			return new CollectionListNodeTranslator() {
				@Override
				@SuppressWarnings("unchecked")
				public Collection<Object> loadListIntoExistingCollection(Node node, LoadContext ctx, Collection<Object> collection) {
					if (collection == null)
						collection = (Collection<Object>)fact.constructCollection(collectionType, node.size());
					else
						collection.clear();
					
					for (Node child: node) {
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
				protected Node saveList(Collection<Object> pojo, Path path, boolean index, SaveContext ctx) {
					
					// If it's empty, might as well skip it - the datastore doesn't store empty lists
					if (pojo.isEmpty())
						throw new SkipException();
					
					Node node = new Node(path);

					if (pojo != null) {
						for (Object obj: pojo) {
							try {
								Node child = componentTranslator.save(obj, path, index, ctx);
								node.addToList(child);
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
			ctx.exitCollection();
		}
	}
}
