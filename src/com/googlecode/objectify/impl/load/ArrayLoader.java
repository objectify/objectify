package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.impl.node.MapNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load an array of things.</p>
 */
public class ArrayLoader implements LoaderFactory<Object>
{
	@Override
	public Loader<Object> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		final Class<?> arrayType = (Class<?>)GenericTypeReflector.erase(type);
		
		if (!arrayType.isArray())
			return null;
		
		final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
		final Loader<?> componentLoader = fact.getLoaders().create(path, fieldAnnotations, componentType);
		
		return new LoaderListNode<Object>(path) {
			@Override
			public Object load(ListNode node, LoadContext ctx) {
				Object array = Array.newInstance(GenericTypeReflector.erase(componentType), node.size());
				
				int index = 0;
				for (MapNode componentNode: node) {
					Object value = componentLoader.load(componentNode, ctx);
					Array.set(array, index++, value);
				}

				return array;
			}
		};
	}
}
