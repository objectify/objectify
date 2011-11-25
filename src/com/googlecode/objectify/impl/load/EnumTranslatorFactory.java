package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;

/**
 * Knows how to convert Enums to the datastore String
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EnumTranslatorFactory extends AbstractValueTranslatorFactory<Enum<?>, String> {
	
	/** */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public EnumTranslatorFactory() {
		super((Class)Enum.class);
	}

	@Override
	protected AbstractValueTranslator<Enum<?>, String> createSafe(ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, final Type type)
	{
		return new AbstractValueTranslator<Enum<?>, String>(path, String.class) {
			@Override
			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Enum<?> loadValue(String value, LoadContext ctx) {
				// Anyone have any idea how to avoid this generics warning?
				return Enum.valueOf((Class<Enum>)type, value.toString());
			}
			
			@Override
			protected String saveValue(Enum<?> value, SaveContext ctx) {
				return value.name();
			}
		};
	}
}