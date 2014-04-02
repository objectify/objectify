package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.impl.Path;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>We just ignore Object, leaving it untranslated. This is TODO</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ObjectTranslatorFactory implements TranslatorFactory<Object, Object>
{
	/* */
	@Override
	public Translator<Object, Object> create(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		if (!type.equals(Object.class))
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
