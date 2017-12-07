package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Value;
import com.googlecode.objectify.impl.Path;


/**
 * <p>Just in case anyone has a {@code Value<?>} field. Just store it as-is.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class RawValueTranslatorFactory implements TranslatorFactory<Value<Object>, Object>
{
	/* */
	@Override
	public Translator<Value<Object>, Object> create(final TypeKey<Value<Object>> tk, final CreateContext ctx, final Path path) {
		if (!tk.isAssignableTo(Value.class))
			return null;

		return new NullSafeTranslator<Value<Object>, Object>() {
			@Override
			protected Value<Object> loadSafe(final Value<Object> value, final LoadContext ctx, final Path path) throws SkipException {
				return value;
			}

			@Override
			protected Value<Object> saveSafe(final Value<Object> pojo, final boolean index, final SaveContext ctx, final Path path) throws SkipException {
				return pojo;
			}
		};
	}
}
