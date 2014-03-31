package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.EmbedMap;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.reflect.Type;
import java.util.Map;


/**
 * <p>Translator which manages an expando-style Map.  The key must be String (or something that converts
 * easily to and from String), which becomes a normal path element in the entity.  The value can be anything
 * that goes into a Collection; a basic type, an embedded type, a reference, a null, etc.  Pretty much the same
 * attribute rules apply as apply to Collections.</p>
 *
 * <p>Map keys cannot contain '.' and cannot be null. The types currently allowed are:</p>
 * <ul>
 * 	<li>String</li>
 *  <li>Key<?> (the Objectify type)</li>
 * </ul>
 * <p>At some point this may be expanded to cover any class that has a String constructor, static valueOf(String),
 * or static create(String) method.</p>
 *
 * <p>However, Maps are not list structures, so you don't have the same restriction on embedding; you
 * can put maps inside of maps and lists inside of maps.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbedMapTranslatorFactory implements TranslatorFactory<Map<Object, Object>>
{
	abstract public static class MapMapNodeTranslator extends MapNodeTranslator<Map<Object, Object>> {
		/** Same as having a null existing collection */
		@Override
		final protected Map<Object, Object> loadMap(Node node, LoadContext ctx) {
			return loadMapIntoExistingMap(node, ctx, null);
		}

		/**
		 * Load into an existing map; allows us to recycle map instances on entities, which might have
		 * exotic concrete types or special initializers (comparators, etc).
		 *
		 * @param coll can be null to trigger creating a new map
		 */
		abstract public Map<Object, Object> loadMapIntoExistingMap(Node node, LoadContext ctx, Map<Object, Object> coll);
	}

	@Override
	public Translator<Map<Object, Object>> create(Path path, final Property property, Type type, CreateContext ctx) {
		if (property.getAnnotation(EmbedMap.class) == null)
			return null;

		@SuppressWarnings("unchecked")
		final Class<? extends Map<?, ?>> mapType = (Class<? extends Map<?, ?>>)GenericTypeReflector.erase(type);

		// This factory ends up being applied to the map value type as well, because the @EmbedMap is still in effect.
		// So we must skip when we find that. Unfortunately this doesn't give us a good way to say "you need to use a
		// Map type with @EmbedMap" in an exception. For now this works. 
		if (!Map.class.isAssignableFrom(mapType))
			return null;

		final Type keyType = GenericTypeReflector.erase(GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]));
		if (keyType != String.class && keyType != Key.class)
			throw new IllegalStateException("@EmbedMap key must be of type String or Key<?>");

		final ObjectifyFactory fact = ctx.getFactory();

		Type componentType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);

		if (componentType.equals(Object.class) || componentType.equals(EmbeddedEntity.class))
			ctx.leaveEmbeddedEntityAloneIfParentHere(path);

		final Translator<Object> componentTranslator = fact.getTranslators().create(path, property, componentType, ctx);

		return new MapMapNodeTranslator() {
			@Override
			@SuppressWarnings("unchecked")
			public Map<Object, Object> loadMapIntoExistingMap(Node node, LoadContext ctx, Map<Object, Object> map) {
				if (map == null)
					map = (Map<Object, Object>)fact.constructMap(mapType);
				else
					map.clear();

				for (Node child: node) {
					String mapKeyString = child.getPath().getSegment();
					
					Object mapKey;
					if (keyType == String.class)
						mapKey = mapKeyString;
					else if (keyType == Key.class)
						mapKey = Key.create(mapKeyString);
					else
						throw new IllegalStateException();	// impossible, checked already
					
					Object value = componentTranslator.load(child, ctx);
					map.put(mapKey, value);
				}

				return map;
			}

			@Override
			protected Node saveMap(Map<Object, Object> pojo, Path path, boolean index, SaveContext ctx) {
				// Note that maps are not like embedded collections; they don't form a list structure so you can embed
				// as many of these as you want.
				Node node = new Node(path);
				node.setPropertyIndexed(index);

				for (Map.Entry<Object, ?> entry: pojo.entrySet()) {
					if (entry.getKey() == null)
						throw new IllegalArgumentException("Map keys cannot be null");

					String key = entry.getKey() instanceof Key ? ((Key<?>)entry.getKey()).getString() : entry.getKey().toString();
					if (key.contains("."))
						throw new IllegalArgumentException("Map keys cannot contain '.' characters");

					Node child = componentTranslator.save(entry.getValue(), path.extend(key), index, ctx);
					node.addToMap(child);
				}

				return node;
			}
		};
	}
}
