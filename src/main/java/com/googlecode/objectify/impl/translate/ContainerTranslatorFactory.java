package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.googlecode.objectify.annotation.Container;
import com.googlecode.objectify.impl.Path;
import lombok.RequiredArgsConstructor;


/**
 * <p>Translator factory which lets users create @Container properties. This is a neat orthogonality
 * in the translation system; @Container properties are just like any other translated property, except
 * that the value is pulled out of the load context instead of the datastore node.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ContainerTranslatorFactory implements TranslatorFactory<Object, Object>
{
	@RequiredArgsConstructor
	private static class ContainerTranslator implements Translator<Object, Object>, Synthetic {
		private final TypeKey<Object> tk;

		@Override
		public Object load(final Value<Object> node, final LoadContext ctx, final Path path) throws SkipException {
			return ctx.getContainer(tk.getType(), path);
		}

		@Override
		public Value<Object> save(final Object pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
			// We never save these
			throw new SkipException();
		}
	}

	@Override
	public Translator<Object, Object> create(final TypeKey<Object> tk, final CreateContext ctx, final Path path) {

		if (!tk.isAnnotationPresent(Container.class))
			return null;

		return new ContainerTranslator(tk);
	}
}
