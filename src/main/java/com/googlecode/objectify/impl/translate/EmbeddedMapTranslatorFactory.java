package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Stringify;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.mapper.Mapper;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.stringifier.NullStringifier;
import com.googlecode.objectify.stringifier.Stringifier;
import com.googlecode.objectify.util.DatastoreUtils;

import javax.xml.soap.Node;


/**
 * <p>Translator which turns a Map<String, ?> into an EmbeddedEntity. As keys in
 * EmbeddedEntity, the map keys must be String or something that can be converted to/from String via a
 * StringConverter (TODO: exact name). The value can be any normal translated value.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedMapTranslatorFactory implements TranslatorFactory<Map<Object, Object>, EmbeddedEntity>
{
	@Override
	public Translator<Map<Object, Object>, EmbeddedEntity> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = (Class<? extends Map<?, ?>>)GenericTypeReflector.erase(type);

		// We apply to any Map
		if (!Map.class.isAssignableFrom(mapType))
			return null;

		Stringify stringify = TypeUtils.getAnnotation(Stringify.class, annotations);

		final Type keyType = GenericTypeReflector.erase(GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]));
		if (keyType != String.class && stringify == null)
			throw new IllegalStateException("Embedded Map keys must be of type String or field must specify @Stringify");

		final ObjectifyFactory fact = ctx.getFactory();

		Type componentType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);

		final Translator<Object, Object> componentTranslator = fact.getTranslators().get(componentType, annotations, ctx, path);

		// Default Stringifier is a null object so we don't have to have special logic
		Class<? extends Stringifier> stringifierClass = stringify == null ? NullStringifier.class : stringify.value();
		@SuppressWarnings("unchecked")
		final Stringifier<Object> stringifier = (Stringifier<Object>)fact.construct(stringifierClass);

		return new TranslatorUsesExistingValue<Map<Object,Object>, EmbeddedEntity>() {

			@Override
			protected Map<Object, Object> load(EmbeddedEntity node, LoadContext ctx, Path path, Map<Object, Object> map) {
				if (map == null)
					map = (Map<Object, Object>)fact.constructMap(mapType);
				else
					map.clear();

				for (Map.Entry<String, Object> entry: node.getProperties().entrySet()) {
					Object key = stringifier.fromString(entry.getKey());
					Object value = componentTranslator.load(entry.getValue(), ctx, path.extend(entry.getKey()));

					map.put(key, value);
				}

				return map;
			}

			@Override
			public EmbeddedEntity save(Map<Object, Object> pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				EmbeddedEntity emb = new EmbeddedEntity();

				for (Map.Entry<Object, Object> entry: pojo.entrySet()) {
					String key = stringifier.toString(entry.getKey());
					Object value = componentTranslator.save(entry.getValue(), index, ctx, path.extend(key));

					DatastoreUtils.setContainerProperty(emb, key, value, index);
				}

				return emb;
			}
		};
	}
}
