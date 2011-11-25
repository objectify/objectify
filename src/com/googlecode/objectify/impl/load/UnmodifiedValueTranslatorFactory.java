package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;


/**
 * <p>This should be the last translator in the list - the one to try when nothing else wants the type.  It just
 * copies the value as-is.  This is used for simple values that get stored natively; GeoPt, Email, etc.  It will
 * also end up trying to save any random value whether it can be saved or not.  The datastore will complain when
 * the entity property is set.</p>
 * 
 * <p>Also - this fixes the boolean.class vs Boolean.class mismatch.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class UnmodifiedValueTranslatorFactory implements TranslatorFactory<Object>
{
	@Override
	public Translator<Object> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {

		return new AbstractValueTranslator<Object, Object>(path, Object.class) {
			@Override
			public Object loadValue(Object value, LoadContext ctx) {
				return value;
			}

			@Override
			protected Object saveValue(Object value, SaveContext ctx) {
				return value;
			}
		};
	}
}
