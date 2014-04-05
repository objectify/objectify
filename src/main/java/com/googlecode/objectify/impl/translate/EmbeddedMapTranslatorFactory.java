package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.stringifier.NullStringifier;
import com.googlecode.objectify.stringifier.Stringifier;
import com.googlecode.objectify.util.DatastoreUtils;
import com.googlecode.objectify.util.GenericUtils;

import java.lang.reflect.Type;
import java.util.Map;


/**
 * <p>Translator which turns a Map<String, ?> into an EmbeddedEntity. As keys in
 * EmbeddedEntity, the map keys must be String or something that can be converted to/from String via a
 * Stringifier. The value can be any normal translated value.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedMapTranslatorFactory implements TranslatorFactory<Map<Object, Object>, EmbeddedEntity>
{
	@Override
	public Translator<Map<Object, Object>, EmbeddedEntity> create(TypeKey<Map<Object, Object>> tk, CreateContext ctx, Path path) {
		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = tk.getTypeAsClass();

		// We apply to any Map
		if (!Map.class.isAssignableFrom(mapType))
			return null;

		Stringify stringify = tk.getAnnotation(Stringify.class);

		final Type keyType = GenericUtils.getMapKeyType(tk.getType());
		if (keyType != String.class && stringify == null)
			throw new IllegalStateException("Embedded Map keys must be of type String or field must specify @Stringify");

		final ObjectifyFactory fact = ctx.getFactory();

		Type componentType = GenericUtils.getMapValueType(mapType);
		final Translator<Object, Object> componentTranslator = fact.getTranslators().get(new TypeKey<>(componentType, tk), ctx, path);

		// Default Stringifier is a null object so we don't have to have special logic
		Class<? extends Stringifier> stringifierClass = stringify == null ? NullStringifier.class : stringify.value();
		@SuppressWarnings("unchecked")
		final Stringifier<Object> stringifier = (Stringifier<Object>)fact.construct(stringifierClass);

		return new TranslatorRecycles<Map<Object,Object>, EmbeddedEntity>() {

			@Override
			public Map<Object, Object> loadInto(EmbeddedEntity node, LoadContext ctx, Path path, Map<Object, Object> into) {
				// Make this work more like collections than atomic values
				if (node == null)
					throw new SkipException();

				if (into == null)
					into = (Map<Object, Object>)fact.constructMap(mapType);
				else
					into.clear();

				for (Map.Entry<String, Object> entry: node.getProperties().entrySet()) {
					Object key = stringifier.fromString(entry.getKey());
					Object value = componentTranslator.load(entry.getValue(), ctx, path.extend(entry.getKey()));

					into.put(key, value);
				}

				return into;
			}

			@Override
			public EmbeddedEntity save(Map<Object, Object> pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				// Make this work more like collections than atomic values
				if (pojo == null || pojo.isEmpty())
					throw new SkipException();

				EmbeddedEntity emb = new EmbeddedEntity();

				for (Map.Entry<Object, Object> entry: pojo.entrySet()) {
					String key = stringifier.toString(entry.getKey());
					Path propPath = path.extend(key);
					Object value = componentTranslator.save(entry.getValue(), index, ctx, propPath);

					DatastoreUtils.setContainerProperty(emb, key, value, index, ctx, propPath);
				}

				return emb;
			}
		};
	}
}
