package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Category;
import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Key;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>This should be near the end of the translation discovery chain. It recognizes any types
 * natively supported by GAE and leaves them as-is when storing in the datastore.</p>
 *
 * <p>Also - this fixes the boolean.class vs Boolean.class mismatch.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AsIsTranslatorFactory implements TranslatorFactory<Object, Object>
{
	/* */
	@Override
	public Translator<Object, Object> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		Class<?> clazz = (Class<?>)GenericTypeReflector.erase(type);

		if (!DataTypeUtils.isSupportedType(clazz))
			return null;

		return new Translator<Object, Object>() {
			@Override
			public Object load(Object node, LoadContext ctx, Path path) throws SkipException {
				return node;
			}

			@Override
			public Object save(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				return pojo;
			}
		};
	}
}
