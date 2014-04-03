package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


/**
 * <p>Translator factory which lets users create @Owner properties. This is a neat orthogonality
 * in the translation system; @Owner properties are just like any other translated property, except
 * that the value is pulled out of the load context instead of the datastore node.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OwnerTranslatorFactory implements TranslatorFactory<Object, Object>
{
	@Override
	public Translator<Object, Object> create(final Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		final Owner owner = TypeUtils.getAnnotation(Owner.class, annotations);

		if (owner == null)
			return null;

		return new Translator<Object, Object>() {
			@Override
			public Object load(Object node, LoadContext ctx, Path path) throws SkipException {
				return ctx.getOwner(type, path);
			}

			@Override
			public Object save(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				// We never save these
				throw new SkipException();
			}
		};
	}
}
