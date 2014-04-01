package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;

import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;

/**
 * The datastore can't store URL, so translate it to a String and back.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class URLTranslatorFactory extends ValueTranslatorFactory<URL, String>
{
	/** */
	public URLTranslatorFactory() {
		super(URL.class);
	}
	
	@Override
	protected ValueTranslator<URL, String> createValueTranslator(Type type, Annotation[] annotations, CreateContext ctx, Path path) {
		return new ValueTranslator<URL, String>(String.class) {
			@Override
			protected URL loadValue(String value, LoadContext ctx, Path path) throws SkipException {
				try {
					return new URL(value);
				} catch (MalformedURLException ex) {
					throw new RuntimeException(ex);
				}
			}

			@Override
			protected String saveValue(URL value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value.toString();
			}
		};
	}
}