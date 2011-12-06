package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;
import java.util.Map;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which manages an expando-style Map.  The key must be String, which becomes a normal path
 * element in the entity.  The value can be anything that goes into a Collection; a basic type, an embedded
 * type, a reference, a null, etc.  Pretty much the same attribute rules apply as apply to Collections.</p>
 * 
 * <p>Map keys cannot contain '.' and cannot be null.</p> 
 * 
 * <p>However, Maps are not list structures, so you don't have the same restriction on embedding; you
 * can put maps inside of maps and lists inside of maps.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MapTranslatorFactory implements TranslatorFactory<Map<String, Object>>
{
	abstract public static class MapMapNodeTranslator extends MapNodeTranslator<Map<String, Object>> {
		/** Same as having a null existing collection */
		@Override
		final protected Map<String, Object> loadMap(Node node, LoadContext ctx) {
			return loadMapIntoExistingMap(node, ctx, null);
		}
		
		/**
		 * Load into an existing map; allows us to recycle map instances on entities, which might have
		 * exotic concrete types or special initializers (comparators, etc).
		 * 
		 * @param coll can be null to trigger creating a new map 
		 */
		abstract public Map<String, Object> loadMapIntoExistingMap(Node node, LoadContext ctx, Map<String, Object> coll);
	}
	
	@Override
	public Translator<Map<String, Object>> create(Path path, Property property, Type type, CreateContext ctx) {
		@SuppressWarnings("unchecked")
		final Class<? extends Map<String, ?>> mapType = (Class<? extends Map<String, ?>>)GenericTypeReflector.erase(type);
		
		if (!Map.class.isAssignableFrom(mapType))
			return null;
		
		Type keyType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]);
		if (keyType != String.class)
			return null;
		
		final ObjectifyFactory fact = ctx.getFactory();
		
		Type componentType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);
		
		final Translator<Object> componentTranslator = fact.getTranslators().create(path, property, componentType, ctx);
		
		return new MapMapNodeTranslator() {
			@Override
			@SuppressWarnings("unchecked")
			public Map<String, Object> loadMapIntoExistingMap(Node node, LoadContext ctx, Map<String, Object> map) {
				if (map == null)
					map = (Map<String, Object>)fact.constructMap(mapType);
				else
					map.clear();
				
				for (final Node child: node) {
					Object value = componentTranslator.load(child, ctx);
					if (value instanceof Result) {
						final Result<?> result = (Result<?>)value;
						final Map<String, Object> finalMap = map;
						
						ctx.defer(new Runnable() {
							@Override
							public void run() {
								finalMap.put(child.getPath().getSegment(), result.now());
							}
						});
					} else {
						map.put(child.getPath().getSegment(), value);
					}
				}

				return map;
			}

			@Override
			protected Node saveMap(Map<String, Object> pojo, Path path, boolean index, SaveContext ctx) {
				// Note that maps are not like embedded collections; they don't form a list structure so you can embed
				// as many of these as you want.
				Node node = new Node(path);
				
				for (Map.Entry<String, ?> entry: pojo.entrySet()) {
					if (entry.getKey() == null)
						throw new IllegalArgumentException("Map keys cannot be null");
					
					if (entry.getKey().contains("."))
						throw new IllegalArgumentException("Map keys cannot contain '.' characters");
						
					Node child = componentTranslator.save(entry.getValue(), path.extend(entry.getKey()), index, ctx);
					node.addToMap(child);
				}
				
				return node;
			}
		};
	}
}
