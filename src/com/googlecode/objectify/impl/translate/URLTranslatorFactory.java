package com.googlecode.objectify.impl.translate;

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
	protected ValueTranslator<URL, String> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<URL, String>(path, String.class) {
			@Override
			protected String saveValue(URL value, SaveContext ctx) {
				return value.toString();
			}
			
			@Override
			protected URL loadValue(String value, LoadContext ctx) {
				try {
					return new URL(value);
				} catch (MalformedURLException ex) {
					throw new RuntimeException(ex);
				}
			}
		};
	}
}