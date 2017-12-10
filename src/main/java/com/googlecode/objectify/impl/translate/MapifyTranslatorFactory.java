package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.ListValue;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Mapify;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.util.GenericUtils;
import com.googlecode.objectify.util.Values;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


/**
 * <p>This takes a datastore collection and converts it to a POJO Map by letting you select out the key value
 * using a class of your own devising. The values will be written to the collection, not the keys.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MapifyTranslatorFactory implements TranslatorFactory<Map<Object, Object>, List<? extends Value<?>>>
{
	@Override
	public Translator<Map<Object, Object>, List<? extends Value<?>>> create(final TypeKey<Map<Object, Object>> tk, final CreateContext ctx, final Path path) {
		final Mapify mapify = tk.getAnnotation(Mapify.class);
		if (mapify == null)
			return null;

		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = tk.getTypeAsClass();

		if (!Map.class.isAssignableFrom(mapType))
			return null;	// We might be here processing the component type of the mapify map!

		final ObjectifyFactory fact = ctx.getFactory();

		final Type componentType = GenericUtils.getMapValueType(tk.getType());
		final Translator<Object, ?> componentTranslator = fact.getTranslators().get(new TypeKey(componentType, tk), ctx, path);

		@SuppressWarnings("unchecked")
		final Mapper<Object, Object> mapper = (Mapper<Object, Object>)fact.construct(mapify.value());

		return new TranslatorRecycles<Map<Object, Object>, List<? extends Value<?>>>() {
			@Override
			public Map<Object, Object> loadInto(final Value<List<? extends Value<?>>> node, final LoadContext ctx, final Path path, Map<Object, Object> map) throws SkipException {
				if (node == null)
					throw new SkipException();

				if (map == null)
					//noinspection unchecked
					map = (Map<Object, Object>)fact.constructMap(mapType);
				else
					map.clear();

				for (final Value<?> child: node.get()) {
					try {
						@SuppressWarnings("unchecked")
						final Object translatedChild = componentTranslator.load((Value)child, ctx, path);
						final Object key = mapper.getKey(translatedChild);
						map.put(key, translatedChild);
					}
					catch (SkipException ex) {
						// No prob, just skip that one
					}
				}

				return map;
			}

			@Override
			public Value<List<? extends Value<?>>> save(final Map<Object, Object> pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {

				// If it's empty, might as well skip it - the datastore doesn't store empty lists
				if (pojo == null || pojo.isEmpty())
					throw new SkipException();

				final List<Value<?>> list = new ArrayList<>(pojo.size());

				for (final Object obj: pojo.values()) {
					try {
						final Value<?> child = componentTranslator.save(obj, index, ctx, path);
						list.add(child);
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
