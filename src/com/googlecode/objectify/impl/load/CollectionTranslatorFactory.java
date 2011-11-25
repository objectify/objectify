package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load things into a collection field.  Might be embedded items, might not.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTranslatorFactory implements TranslatorFactory<Collection<?>>
{
	@Override
	public Translator<Collection<?>> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		@SuppressWarnings("unchecked")
		final Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>)GenericTypeReflector.erase(type);
		
		if (!Collection.class.isAssignableFrom(collectionType))
			return null;
		
		Type componentType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
		final Translator<Object> componentTranslator = fact.getLoaders().create(path, fieldAnnotations, componentType);
		
		final boolean embedded = TypeUtils.getAnnotation(Embed.class, fieldAnnotations, GenericTypeReflector.erase(componentType)) != null;
		
		return new ListNodeTranslator<Collection<?>>(path) {
			@Override
			public Collection<?> loadList(ListNode node, LoadContext ctx) {
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>)fact.constructCollection(collectionType, node.size());
				
				for (EntityNode child: node) {
					Object value = componentTranslator.load(child, ctx);
					collection.add(value);
				}

				return collection;
			}

			@Override
			protected ListNode saveList(Collection<?> pojo, boolean index, SaveContext ctx) {
				// We need to be careful to note when we are in embedded collections, because some features work
				// differently (or not at all).  In particular, String->Text conversion.
				if (embedded)
					ctx.setInEmbeddedCollection(true);

				try {
					ListNode node = new ListNode(path);
					
					for (Object obj: pojo) {
						EntityNode child = componentTranslator.save(obj, index, ctx);
						node.add(child);
					}
					
					return node;
					
				} finally {
					if (embedded)
						ctx.setInEmbeddedCollection(false);
				}
			}
		};
	}
}
