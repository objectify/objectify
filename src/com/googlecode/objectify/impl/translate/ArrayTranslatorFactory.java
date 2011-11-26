package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
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
	public Translator<Object> create(Path path, Annotation[] fieldAnnotations, Type type, CreateContext ctx) {
		final Class<?> arrayType = (Class<?>)GenericTypeReflector.erase(type);
		
		if (!arrayType.isArray())
			return null;

		ctx.setInCollection(true);
		try {
			final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
			final Translator<Object> componentTranslator = ctx.getFactory().getTranslators().create(path, fieldAnnotations, componentType);
	
			return new ListNodeTranslator<Object>(path) {
				@Override
				public Object loadList(ListNode node, LoadContext ctx) {
					List<Object> list = new ArrayList<Object>(node.size());
					
					for (EntityNode componentNode: node) {
						try {
							Object value = componentTranslator.load(componentNode, ctx);
							list.add(value);
						}
						catch (SkipException ex) {
							// No prob skip that one
						}
					}
	
					// We can't use List.toArray() because it doesn't work with primitives
					Object array = Array.newInstance(GenericTypeReflector.erase(componentType), list.size());
					for (int i=0; i<list.size(); i++)
						Array.set(array, i, list.get(i));
					
					return array;
				}
				
				@Override
				protected ListNode saveList(Object pojo, boolean index, SaveContext ctx) {
					// If the array is null, just skip it.  This is important because of the way filtering works;
					// if we stored a null then the field would match when filtering for null (same as a null in the list).
					if (pojo == null)
						throw new SkipException();
					
					int len = Array.getLength(pojo);

					// If it's empty, might as well skip it - the datastore doesn't store empty lists
					if (len == 0)
						throw new SkipException();
					
					ListNode node = new ListNode(path);
					
					for (int i=0; i<len; i++) {
						try {
							Object value = Array.get(pojo, i);
							EntityNode addNode = componentTranslator.save(value, index, ctx);
							node.add(addNode);
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
			ctx.setInCollection(false);
		}
	}
}
