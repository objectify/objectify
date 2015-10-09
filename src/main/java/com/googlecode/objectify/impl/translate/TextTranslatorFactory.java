package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;


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
	protected ValueTranslator<Text, Object> createValueTranslator(TypeKey<Text> tk, CreateContext ctx, Path path) {
		// Should never be part of a projection since Text cannot be indexed, but just in case someone is converting
		// a String to a Text field. Unlikely but it doesn't hurt to pass the String.class here.
		return new ValueTranslator<Text, Object>(Object.class, String.class) {
			@Override
			protected Text loadValue(Object value, LoadContext ctx, Path path) throws SkipException {
				if (value instanceof Text)
					return (Text)value;
				else
					return new Text(value.toString());
			}

			@Override
			protected Object saveValue(Text value, boolean index, SaveContext ctx, Path path) throws SkipException {
				return value;
			}
		};
	}
}
