package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load things into a collection field.  Might be embedded items, might not.</p>
 */
public class CollectionLoader implements LoaderFactory<Collection<?>>
{
	@Override
	public Loader<Collection<?>> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		final Class<?> collectionType = (Class<?>)GenericTypeReflector.erase(type);
		
		if (!Collection.class.isAssignableFrom(collectionType))
			return null;
		
		Type componentType = GenericTypeReflector.getTypeParameter(type, Collection.class.getTypeParameters()[0]);
		final Loader<?> componentLoader = fact.getLoaders().create(path, fieldAnnotations, componentType);
		
		return new LoaderListNode<Collection<?>>(path) {
			@Override
			public Collection<?> load(ListNode node, LoadContext ctx) {
				@SuppressWarnings("unchecked")
				Collection<Object> collection = (Collection<Object>)fact.construct_turnthisintoprepare(collectionType);
				
				for (EntityNode child: node) {
					Object value = componentLoader.load(child, ctx);
					collection.add(value);
				}

				return collection;
			}
		};
	}
}
