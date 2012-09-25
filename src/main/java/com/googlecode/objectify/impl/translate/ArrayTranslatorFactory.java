package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which can load an array of things.</p>
 * 
 * <p>Note that empty or null arrays are not stored in the datastore, and null values for the array
 * field are ignored when they are loaded from the Entity.  This is because the datastore doesn't store empty
 * collections, and storing null fields will confuse filtering for actual nulls in the array contents.</p>
 * 
 * @see CollectionTranslatorFactory
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ArrayTranslatorFactory implements TranslatorFactory<Object>
{
	@Override
	public Translator<Object> create(Path path, final Property property, Type type, CreateContext ctx) {
		final Class<?> arrayType = (Class<?>)GenericTypeReflector.erase(type);
		
		if (!arrayType.isArray())
			return null;

		ctx.enterCollection(path);
		try {
			final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
			final Translator<Object> componentTranslator = ctx.getFactory().getTranslators().create(path, property, componentType, ctx);
	
			return new ListNodeTranslator<Object>() {
				@Override
				public Object loadList(Node node, LoadContext ctx) {
					List<Object> list = new ArrayList<Object>(node.size());
					
					for (Node componentNode: node) {
						try {
							Object value = componentTranslator.load(componentNode, ctx);
							list.add(value);
						}
						catch (SkipException ex) {
							// No prob skip that one
						}
					}
	
					// We can't use List.toArray() because it doesn't work with primitives
					final Object array = Array.newInstance(GenericTypeReflector.erase(componentType), list.size());
					for (int i=0; i<list.size(); i++) {
						final Object value = list.get(i);
						final int index = i;
						
						if (value instanceof Result) {
							// defer the set operation
							ctx.deferA(new Runnable() {
								@Override
								public void run() {
									Array.set(array, index, ((Result<?>)value).now());
								}
							});
						} else {
							Array.set(array, i, value);
						}
						
					}
					
					return array;
				}
				
				@Override
				protected Node saveList(Object pojo, Path path, boolean index, SaveContext ctx) {
					int len = Array.getLength(pojo);

					// If it's empty, might as well skip it - the datastore doesn't store empty lists
					if (len == 0)
						throw new SkipException();
					
					Node node = new Node(path);
					
					for (int i=0; i<len; i++) {
						try {
							Object value = Array.get(pojo, i);
							Node addNode = componentTranslator.save(value, path, index, ctx);
							node.addToList(addNode);
						}
						catch (SkipException ex) {
							// No problem, skip that element
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
