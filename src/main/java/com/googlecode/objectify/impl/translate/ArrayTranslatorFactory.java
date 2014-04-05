package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * <p>Translator which can load an array of things.</p>
 *
 * <p>Note that empty or null arrays are not stored in the datastore, and null values for the array
 * field are ignored when they are loaded from the Entity.  This is because the datastore doesn't store empty
 * collections, and storing null fields will confuse filtering for actual nulls in the array contents.</p>
 *
 * <p>The reason the generic P type of this factory is Object instead of Object[] is that Object[]
 * is incompatible with the primitive arrays. This factory handles primitives as well.</p>
 *
 * @see CollectionTranslatorFactory
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ArrayTranslatorFactory implements TranslatorFactory<Object, Collection<Object>>
{
	@Override
	public Translator<Object, Collection<Object>> create(Type type, Annotation[] annotations, final CreateContext ctx, final Path path) {
		final Class<?> arrayType = (Class<?>)GenericTypeReflector.erase(type);

		if (!arrayType.isArray())
			return null;

		final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
		final Translator<Object, Object> componentTranslator = ctx.getTranslator(componentType, annotations, ctx, path);

		return new Translator<Object, Collection<Object>>() {
			@Override
			public Object load(Collection<Object> node, LoadContext ctx, Path path) throws SkipException {
				if (node == null)
					throw new SkipException();

				List<Object> list = new ArrayList<Object>(node.size());

				for (Object componentNode: node) {
					try {
						Object value = componentTranslator.load(componentNode, ctx, path);
						list.add(value);
					}
					catch (SkipException ex) {
						// No prob skip that one
					}
				}

				// We can't use List.toArray() because it doesn't work with primitives
				final Object array = Array.newInstance(GenericTypeReflector.erase(componentType), list.size());
				for (int i=0; i<list.size(); i++) {
					Array.set(array, i, list.get(i));
				}

				return array;
			}

			@Override
			public Collection<Object> save(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				// Use same behavior as collections.
				if (pojo == null)
					throw new SkipException();

				int len = Array.getLength(pojo);

				// If it's empty, might as well skip it - the datastore doesn't store empty lists
				if (len == 0)
					throw new SkipException();

				List<Object> list = new ArrayList<>(len);

				for (int i=0; i<len; i++) {
					try {
						Object value = Array.get(pojo, i);
						Object addNode = componentTranslator.save(value, index, ctx, path);
						list.add(addNode);
					}
					catch (SkipException ex) {
						// No problem, skip that element
					}
				}

				return list;
			}
		};
	}
}
