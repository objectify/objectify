package com.googlecode.objectify.impl.translate;

import com.googlecode.objectify.annotation.Owner;
import com.googlecode.objectify.impl.Path;


/**
 * <p>Translator factory which lets users create @Owner properties. This is a neat orthogonality
 * in the translation system; @Owner properties are just like any other translated property, except
 * that the value is pulled out of the load context instead of the datastore node.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class OwnerTranslatorFactory implements TranslatorFactory<Object, Object>
{
	private static class OwnerTranslator implements Translator<Object, Object>, Synthetic {
		private final TypeKey<Object> tk;

		public OwnerTranslator(TypeKey<Object> tk) {
			this.tk = tk;
		}

		@Override
		public Object load(Object node, LoadContext ctx, Path path) throws SkipException {
			return ctx.getOwner(tk.getType(), path);
		}

		@Override
		public Object save(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
			// We never save these
			throw new SkipException();
		}
	}

	@Override
	public Translator<Object, Object> create(TypeKey<Object> tk, CreateContext ctx, Path path) {

		if (!tk.isAnnotationPresent(Owner.class))
			return null;

		return new OwnerTranslator(tk);
	}
}
