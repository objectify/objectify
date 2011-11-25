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
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTranslatorFactory implements TranslatorFactory<Collection<Object>>
{
	abstract public static class CollectionListNodeTranslator<T> extends ListNodeTranslator<Collection<T>> {
		public CollectionListNodeTranslator(Path path) {
			super(path);
		}
		
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
			final Translator<Object> componentTranslator = fact.getTranslators().create(path, fieldAnnotations, componentType);
			
			return new CollectionListNodeTranslator<Object>(path) {
				@Override
				@SuppressWarnings("unchecked")
				public Collection<Object> loadListIntoExistingCollection(ListNode node, LoadContext ctx, Collection<Object> collection) {
					if (collection == null)
						collection = (Collection<Object>)fact.constructCollection(collectionType, node.size());
					else
						collection.clear();
					
					for (EntityNode child: node) {
						Object value = componentTranslator.load(child, ctx);
						collection.add(value);
					}
	
					return collection;
				}
	
				@Override
				protected ListNode saveList(Collection<Object> pojo, boolean index, SaveContext ctx) {
					ListNode node = new ListNode(path);
					
					for (Object obj: pojo) {
						EntityNode child = componentTranslator.save(obj, index, ctx);
						node.add(child);
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
