package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.PropertyContainer;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>Translator which can map whole embedded classes. Embedded classes are just like entities but
 * they lack keys. This is a "catch-all" factory; it will always return a translator that does its
 * best to hack apart the object field-by-field. This factory should be the *last* factory in the
 * discovery chain.</p>
 *
 * <p>Note that this means that any class embedded in another class is treated as embedded.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbeddedClassTranslatorFactory<P> implements TranslatorFactory<P, PropertyContainer>
{
	@Override
	public Translator<P, PropertyContainer> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		Class<P> clazz = (Class<P>)GenericTypeReflector.erase(type);
		return new EmbeddedClassTranslator<P>(clazz, ctx, path);
	}
}
