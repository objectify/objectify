package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;
import java.util.Map;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>This takes a collection and converts it to a map by letting you select out the key value
 * using a class of your own devising.  All the rules for collections normally apply (ie, you
 * can't have collections inside of collections) but otherwise this works just like a map.  The
 * values will be written to the collection, not the keys.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MapifyTranslatorFactory implements TranslatorFactory<Map<Object, Object>>
{
	abstract public static class MapifyListNodeTranslator extends ListNodeTranslator<Map<Object, Object>> {

		/** Same as having a null existing map */
		@Override
		final protected Map<Object, Object> loadList(Node node, LoadContext ctx) {
			return loadListIntoExistingMap(node, ctx, null);
		}

		/**
		 * Load into an existing map; allows us to recycle map instances on entities, which might have
		 * exotic concrete types or special initializers (comparators, etc).
		 *
		 * @param map can be null to trigger creating a new map
		 */
		abstract public Map<Object, Object> loadListIntoExistingMap(Node node, LoadContext ctx, Map<Object, Object> map);
	}

	@Override
	public Translator<Map<Object, Object>> create(Path path, final Property property, Type type, CreateContext ctx) {
		Mapify mapify = property.getAnnotation(Mapify.class);
		if (mapify == null)
			return null;

		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = (Class<? extends Map<?, ?>>)GenericTypeReflector.erase(type);

		if (!Map.class.isAssignableFrom(mapType))
			return null;	// We might be here processing the component type of the mapify map!

		ctx.enterCollection(path);
		try {
			final ObjectifyFactory fact = ctx.getFactory();

			Type componentType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);
			if (componentType == null)	// if it was a raw type, just assume Object
				componentType = Object.class;

			final Translator<Object> componentTranslator = fact.getTranslators().create(path, property, componentType, ctx);

			@SuppressWarnings("unchecked")
			final Mapper<Object, Object> mapper = (Mapper<Object, Object>)fact.construct(mapify.value());

			return new MapifyListNodeTranslator() {
				@Override
				@SuppressWarnings("unchecked")
				public Map<Object, Object> loadListIntoExistingMap(Node node, LoadContext ctx, Map<Object, Object> map) {
					if (map == null)
						map = (Map<Object, Object>)fact.constructMap(mapType);
					else
						map.clear();

					for (Node child: node) {
						try {
							final Map<Object, Object> finalMap = map;
							Object value = componentTranslator.load(child, ctx);

							if (value instanceof Result) {
								// We need to defer the add
								final Result<?> result = ((Result<?>)value);

								ctx.deferA(new Runnable() {
									@Override
									public void run() {
										Object key = mapper.getKey(result.now());
										finalMap.put(key, result.now());
									}
								});
							} else {
								Object key = mapper.getKey(value);
								finalMap.put(key, value);
							}
						}
						catch (SkipException ex) {
							// No prob, just skip that one
						}
					}

					return map;
				}

				@Override
				protected Node saveList(Map<Object, Object> pojo, Path path, boolean index, SaveContext ctx) {

					// If it's empty, might as well skip it - the datastore doesn't store empty lists
					if (pojo == null || pojo.isEmpty())
						throw new SkipException();

					Node node = new Node(path);

					for (Object obj: pojo.values()) {
						try {
							Node child = componentTranslator.save(obj, path, index, ctx);
							node.addToList(child);
						}
						catch (SkipException ex) {
							// Just skip that node, no prob
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
