package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


/**
 * Knows how to convert Texts.  Aside from String and Text, will work with anything that's in the
 * datastore just by calling toString() on what we get back; convenient for converting between
 * say Number and the Text representation, possibly dangerous otherwise. 
 */
public class TextTranslatorFactory extends ValueTranslatorFactory<Text, Object>
{
	/** */
	public TextTranslatorFactory() {
		super(Text.class);
	}
	
	@Override
	protected ValueTranslator<Text, Object> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		return new ValueTranslator<Text, Object>(path, Object.class) {
			@Override
			protected Text loadValue(Object value, LoadContext ctx) {
				if (value instanceof Text)
					return (Text)value;
				else
					return new Text(value.toString());
			}
			
			@Override
			protected Object saveValue(Text value, SaveContext ctx) {
				return value;
			}
		};
	}
}