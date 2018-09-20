package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.util.GenericUtils;
import com.googlecode.objectify.util.Values;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


/**
 * <p>Translator which can load things into a collection field. Those things are themselves translated.</p>
 *
 * <p>This translator is clever about recycling an existing collection in the POJO field when loading.
 * That way a collection that has been initialized with a sort (or other data) will remain intact.</p>
 *
 * <p>Note that empty or null collections are not stored in the datastore, and null values for the collection
 * field are ignored when they are loaded from the Entity.  This is because the datastore doesn't store empty
 * collections, and storing null fields will confuse filtering for actual nulls in the collection contents.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class CollectionTranslatorFactory implements TranslatorFactory<Collection<Object>, List<? extends Value<?>>>
{
	@Override
	public Translator<Collection<Object>, List<? extends Value<?>>> create(final TypeKey<Collection<Object>> tk, final CreateContext ctx, final Path path) {
		@SuppressWarnings("unchecked")
		final Class<? extends Collection<?>> collectionType = tk.getTypeAsClass();

		if (!Collection.class.isAssignableFrom(collectionType))
			return null;

		final ObjectifyFactory fact = ctx.getFactory();

		final Type componentType = GenericUtils.getCollectionComponentType(tk.getType());
		final Translator<Object, ?> componentTranslator = ctx.getTranslator(new TypeKey<>(componentType, tk), ctx, path);

		return new TranslatorRecycles<Collection<Object>, List<? extends Value<?>>>() {

			@Override
			public Collection<Object> loadInto(final Value<List<? extends Value<?>>> node, final LoadContext ctx, final Path path, Collection<Object> collection) throws SkipException {
				// If the collection does not exist, skip it entirely. This mirrors the OLD underlying behavior
				// of collections in the datastore; if they are empty, they don't exist.
				if (node == null || node.get() == null)
					throw new SkipException();

				if (collection == null)
					//noinspection unchecked
					collection = (Collection<Object>)fact.constructCollection(collectionType, node.get().size());
				else
					collection.clear();

				for (final Value<?> child: node.get()) {
					try {
						@SuppressWarnings("unchecked")
						final Object value = componentTranslator.load((Value)child, ctx, path);
						collection.add(value);
					}
					catch (SkipException ex) {
						// No prob, just skip that one
					}
				}

				return collection;
			}

			@Override
			public Value<List<? extends Value<?>>> save(final Collection<Object> pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {

				// If it's empty, might as well skip it - the datastore doesn't store empty lists
				if (pojo == null || pojo.isEmpty())
					throw new SkipException();

				final List<Value<?>> list = new ArrayList<>();

				for (final Object obj: pojo) {
					try {
						final Value<?> translatedChild = componentTranslator.save(obj, index, ctx, path);
						list.add(translatedChild);
					}
					catch (SkipException ex) {
						// Just skip that node, no prob
					}
				}

				Values.homogenizeIndexes(list);
				return ListValue.of(list);
			}
		};
	}
}
