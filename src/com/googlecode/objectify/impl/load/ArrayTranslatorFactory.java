package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.ListNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which can load an array of things.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ArrayTranslatorFactory implements TranslatorFactory<Object>
{
	@Override
	public Translator<Object> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {
		final Class<?> arrayType = (Class<?>)GenericTypeReflector.erase(type);
		
		if (!arrayType.isArray())
			return null;
		
		final Type componentType = GenericTypeReflector.getArrayComponentType(arrayType);
		final Translator<Object> componentTranslator = fact.getLoaders().create(path, fieldAnnotations, componentType);
		
		return new ListNodeTranslator<Object>(path) {
			@Override
			public Object loadList(ListNode node, LoadContext ctx) {
				Object array = Array.newInstance(GenericTypeReflector.erase(componentType), node.size());
				
				int index = 0;
				for (EntityNode componentNode: node) {
					Object value = componentTranslator.load(componentNode, ctx);
					Array.set(array, index++, value);
				}

				return array;
			}
			
			@Override
			protected ListNode saveList(Object pojo, boolean index, SaveContext ctx) {
				ListNode node = new ListNode(path);
				int len = Array.getLength(pojo);
				for (int i=0; i<len; i++) {
					Object value = Array.get(pojo, i);
					EntityNode addNode = componentTranslator.save(value, index, ctx);
					node.add(addNode);
				}
				return null;
			}
		};
	}
}
