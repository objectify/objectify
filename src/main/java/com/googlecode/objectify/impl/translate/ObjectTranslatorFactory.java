package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.Path;
import lombok.RequiredArgsConstructor;


/**
 * <p>In case someone has a field of type Object or uses a raw collection without specifying the generic type.
 * This is less efficient because we have to dynamically figure out the type, but at least it works.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@RequiredArgsConstructor
public class ObjectTranslatorFactory implements TranslatorFactory<Object, Object>
{
	private final Translators translators;

	/* */
	@Override
	public Translator<Object, Object> create(final TypeKey<Object> tk, final CreateContext createCtx, final Path path) {
		if (tk.getTypeAsClass() != Object.class)
			return null;

		return new NullSafeTranslator<Object, Object>() {
			@Override
			protected Object loadSafe(final Value<Object> value, final LoadContext ctx, final Path path) throws SkipException {
				return value.get();
			}

			@Override
			protected Value<Object> saveSafe(final Object pojo, final boolean index, final SaveContext saveCtx, final Path path) throws SkipException {
				final TypeKey<Object> runtimeTypeKey = new TypeKey<>(pojo.getClass());

				// We really only need the createctx so that the translators can get a factory.
				final Translator<Object, Object> realTranslator = translators.get(runtimeTypeKey, createCtx, path);
				return realTranslator.save(pojo, index, saveCtx, path);
			}
		};
	}
}
