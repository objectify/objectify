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
	public Translator<Object, Object> create(TypeKey tk, CreateContext ctx, Path path) {
		Class<?> clazz = tk.getTypeAsClass();

		if (!(clazz == Object.class || DataTypeUtils.isSupportedType(clazz)))
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
