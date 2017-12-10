package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.Values;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
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
public class ArrayTranslatorFactory implements TranslatorFactory<Object, List<? extends Value<?>>>
{
	@Override
	public Translator<Object, List<? extends Value<?>>> create(final TypeKey<Object> tk, final CreateContext ctx, final Path path) {
		final Class<?> arrayType = tk.getTypeAsClass();

		if (!arrayType.isArray())
			return null;

		final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
		final Translator<Object, ?> componentTranslator = ctx.getTranslator(new TypeKey<>(componentType, tk), ctx, path);

		return new Translator<Object, List<? extends Value<?>>>() {
			@Override
			public Object load(final Value<List<? extends Value<?>>> node, final LoadContext ctx, final Path path) throws SkipException {
				if (node == null)
					throw new SkipException();

				final List<Object> list = new ArrayList<>(node.get().size());

				for (final Value<?> componentNode: node.get()) {
					try {
						@SuppressWarnings("unchecked")
						final Object value = componentTranslator.load((Value)componentNode, ctx, path);
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
			public Value<List<? extends Value<?>>> save(final Object pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
				// Use same behavior as collections.
				if (pojo == null)
					throw new SkipException();

				final int len = Array.getLength(pojo);

				// If it's empty, might as well skip it - the datastore doesn't store empty lists
				if (len == 0)
					throw new SkipException();

				final List<Value<?>> list = new ArrayList<>(len);

				for (int i=0; i<len; i++) {
					try {
						final Object value = Array.get(pojo, i);
						final Value<?> addNode = componentTranslator.save(value, index, ctx, path);
						list.add(addNode);
					}
					catch (SkipException ex) {
						// No problem, skip that element
					}
				}

				Values.homogenizeIndexes(list);
				return ListValue.of(list);
			}
		};
	}
}
