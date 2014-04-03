package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.GenericUtils;

import java.lang.annotation.Annotation;
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
public class CollectionTranslatorFactory implements TranslatorFactory<Collection<Object>, Collection<Object>>
{
	@Override
	public Translator<Collection<Object>, Collection<Object>> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		@SuppressWarnings("unchecked")
		final Class<? extends Collection<?>> collectionType = (Class<? extends Collection<?>>)GenericTypeReflector.erase(type);

		if (!Collection.class.isAssignableFrom(collectionType))
			return null;

		final ObjectifyFactory fact = ctx.getFactory();

		Type componentType = GenericUtils.getCollectionComponentType(type);
		final Translator<Object, Object> componentTranslator = ctx.getTranslator(componentType, annotations, ctx, path);

		return new TranslatorRecycles<Collection<Object>, Collection<Object>>() {

			@Override
			public Collection<Object> loadInto(Collection<Object> node, LoadContext ctx, Path path, Collection<Object> collection) throws SkipException {
				// If the collection does not exist, skip it entirely. This mirrors the underlying behavior
				// of collections in the datastore; if they are empty, they don't exist.
				if (node == null)
					throw new SkipException();

				if (collection == null)
					collection = (Collection<Object>)fact.constructCollection(collectionType, node.size());
				else
					collection.clear();

				for (Object child: node) {
					try {
						Object value = componentTranslator.load(child, ctx, path);
						collection.add(value);
					}
					catch (SkipException ex) {
						// No prob, just skip that one
					}
				}

				return collection;
			}

			@Override
			public Collection<Object> save(Collection<Object> pojo, boolean index, SaveContext ctx, Path path) throws SkipException {

				// If it's empty, might as well skip it - the datastore doesn't store empty lists
				if (pojo == null || pojo.isEmpty())
					throw new SkipException();

				List<Object> list = new ArrayList<>();

				for (Object obj: pojo) {
					try {
						Object translatedChild = componentTranslator.save(obj, index, ctx, path);
						list.add(translatedChild);
					}
					catch (SkipException ex) {
						// Just skip that node, no prob
					}
				}

				return list;
			}
		};
	}
}
