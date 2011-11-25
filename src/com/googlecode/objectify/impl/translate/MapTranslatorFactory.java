package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.MapNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which manages an expando-style Map.  The key must be String, which becomes a normal path
 * element in the entity.  The value can be anything that goes into a Collection; a basic type, an embedded
 * type, a reference, etc.  Pretty much the same attribute rules apply as apply to Collections.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class MapTranslatorFactory implements TranslatorFactory<Map<String, ?>>
{
	@Override
	public Translator<Map<String, ?>> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		@SuppressWarnings("unchecked")
		final Class<? extends Map<String, ?>> mapType = (Class<? extends Map<String, ?>>)GenericTypeReflector.erase(type);
		
		if (!Map.class.isAssignableFrom(mapType))
			return null;
		
		Type keyType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]);
		if (keyType != String.class)
			return null;
		
		Type componentType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);
		
		final Translator<Object> componentTranslator = fact.getTranslators().create(path, fieldAnnotations, componentType);
		
		final boolean embedded = TypeUtils.getAnnotation(Embed.class, fieldAnnotations, GenericTypeReflector.erase(componentType)) != null;
		
		return new MapNodeTranslator<Map<String, ?>>(path) {
			@Override
			protected Map<String, ?> loadMap(MapNode node, LoadContext ctx) {
				@SuppressWarnings("unchecked")
				Map<String, Object> map = (Map<String, Object>)fact.constructMap(mapType);
				
				for (Map.Entry<String, EntityNode> entry: node.entrySet()) {
					Object value = componentTranslator.load(entry.getValue(), ctx);
					map.put(entry.getKey(), value);
				}

				return map;
			}

			@Override
			protected MapNode saveMap(Map<String, ?> pojo, boolean index, SaveContext ctx) {
				// We need to be careful to note when we are in embedded collections, because some features work
				// differently (or not at all).  In particular, String->Text conversion.
				if (embedded)
					ctx.setInEmbeddedCollection(true);

				try {
					MapNode node = new MapNode(path);
					
					for (Map.Entry<String, ?> entry: pojo.entrySet()) {
						
						EntityNode child = componentTranslator.save(entry.getValue(), index, ctx);
						node.put(entry.getKey(), child);
					}
					
					return node;
					
				} finally {
					if (embedded)
						ctx.setInEmbeddedCollection(false);
				}
			}
		};
	}
}
