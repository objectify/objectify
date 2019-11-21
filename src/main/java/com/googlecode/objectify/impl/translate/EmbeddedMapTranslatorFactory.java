package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.FullEntity;
import com.google.cloud.datastore.Value;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.stringifier.EnumStringifier;
import com.googlecode.objectify.stringifier.InitializeStringifier;
import com.googlecode.objectify.stringifier.KeyStringifier;
import com.googlecode.objectify.stringifier.NullStringifier;
import com.googlecode.objectify.stringifier.Stringifier;
import com.googlecode.objectify.util.GenericUtils;

import java.lang.reflect.Type;
import java.util.Map;


/**
 * <p>Translator which turns a Map&lt;String, ?&gt; into an EmbeddedEntity. As keys in
 * EmbeddedEntity, the map keys must be String or something that can be converted to/from String via a
 * Stringifier. The value can be any normal translated value.</p>
 *
 * <p>This automatically stringifies Enums and objectify Key&lt;?&gt;s</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedMapTranslatorFactory implements TranslatorFactory<Map<Object, Object>, FullEntity<?>>
{
	@Override
	public Translator<Map<Object, Object>, FullEntity<?>> create(final TypeKey<Map<Object, Object>> tk, final CreateContext ctx, final Path path) {
		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = tk.getTypeAsClass();

		// We apply to any Map
		if (!Map.class.isAssignableFrom(mapType))
			return null;

		final Stringify stringify = tk.getAnnotation(Stringify.class);

		final Type keyType = GenericUtils.getMapKeyType(tk.getType());
		final Class<?> keyTypeErased = GenericTypeReflector.erase(keyType);

		final Class<? extends Stringifier> stringifierClass;
		if (stringify != null)
			stringifierClass = stringify.value();
		else if (keyTypeErased == String.class)
			stringifierClass = NullStringifier.class;
		else if (Enum.class.isAssignableFrom(keyTypeErased))
			stringifierClass = EnumStringifier.class;
		else if (keyTypeErased == Key.class)
			stringifierClass = KeyStringifier.class;
		else
			throw new IllegalStateException("Embedded Map keys must be of type String/Enum/Key<?> or field must specify @Stringify");

		final ObjectifyFactory fact = ctx.getFactory();

		@SuppressWarnings("unchecked")
		final Stringifier<Object> stringifier = (Stringifier<Object>)fact.construct(stringifierClass);
		if (stringifier instanceof InitializeStringifier)
			((InitializeStringifier)stringifier).init(fact, keyType);

		final Type componentType = GenericUtils.getMapValueType(tk.getType());
		final Translator<Object, ?> componentTranslator = fact.getTranslators().get(new TypeKey<>(componentType, tk), ctx, path);

		return new TranslatorRecycles<Map<Object,Object>, FullEntity<?>>() {

			@Override
			public Map<Object, Object> loadInto(final Value<FullEntity<?>> node, final LoadContext ctx, final Path path, Map<Object, Object> into) {
				// Make this work more like collections than atomic values
				if (node == null)
					throw new SkipException();

				if (into == null)
					//noinspection unchecked
					into = (Map<Object, Object>)fact.constructMap(mapType);
				else
					into.clear();

				for (final String name : node.get().getNames()) {
					final Object key = stringifier.fromString(name);
					final Value<?> nodeValue = node.get().getValue(name);

					@SuppressWarnings("unchecked")
					final Object value = componentTranslator.load((Value)nodeValue, ctx, path.extend(name));

					into.put(key, value);
				}

				return into;
			}

			@Override
			public Value<FullEntity<?>> save(final Map<Object, Object> pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
				// Make this work more like collections than atomic values
				if (pojo == null || pojo.isEmpty())
					throw new SkipException();

				final FullEntity.Builder<?> emb = FullEntity.newBuilder();

				for (final Map.Entry<Object, Object> entry: pojo.entrySet()) {
					try {
						final String key = stringifier.toString(entry.getKey());
						if (key == null)
							path.throwNullPointer("null is not allowed as a map key");

						final Path propPath = path.extend(key);
						final Value<?> value = componentTranslator.save(entry.getValue(), index, ctx, propPath);

						emb.set(key, value);
					} catch (SkipException e) {
						// do nothing
					}
				}

				return EntityValue.of(emb.build());
			}
		};
	}
}
