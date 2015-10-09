package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.googlecode.objectify.impl.Path;


/**
 * <p>This should be near the end of the translation discovery chain. It recognizes any types
 * natively supported by GAE and leaves them as-is when storing in the datastore.</p>
 *
 * <p>Also handles the case of a field declared Object; we leave it unmolested.</p>
 * <p>Also - this fixes the boolean.class vs Boolean.class mismatch.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class AsIsTranslatorFactory implements TranslatorFactory<Object, Object>
{
	/* */
	@Override
	public Translator<Object, Object> create(TypeKey<Object> tk, CreateContext ctx, Path path) {
		Class<Object> clazz = tk.getTypeAsClass();

		if (!(clazz == Object.class || clazz.isPrimitive() || DataTypeUtils.isSupportedType(clazz)))
			return null;

		return new ProjectionSafeTranslator<Object, Object>(clazz) {
			@Override
			protected Object loadSafe2(Object value, LoadContext ctx, Path path) throws SkipException {
				return value;
			}

			@Override
			protected Object saveSafe(Object pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				return pojo;
			}
		};
	}
}
