package com.googlecode.objectify.impl.conv;

import com.google.appengine.api.datastore.Text;


/**
 * Knows how to convert Strings.  Datastore representation might be String or it might be Text.
 * Will work with anything that's in the datastore just by calling toString() on what we get back;
 * convenient for converting between say Number and the String representation, possibly dangerous
 * otherwise. 
 */
public class StringConverter extends SimpleConverterFactory<String, Object>
{
	/** */
	public StringConverter() {
		super(String.class);
	}
	
	@Override
	public Converter<String, Object> create(Class<?> type, ConverterCreateContext ctx) {
		return new Converter<String, Object>() {
			/* */
			@Override
			public String toPojo(Object value, ConverterLoadContext ctx) {
				if (value instanceof Text)
					return ((Text)value).getValue();
				else
					return value.toString();
			}
			
			/* */
			@Override
			public Object toDatastore(String value, ConverterSaveContext ctx) {
				// Check to see if it's too long and needs to be Text instead
				if (value.length() > 500) {
					if (ctx.inEmbeddedCollection())
						throw new IllegalStateException(ctx.createErrorMessage("Objectify cannot autoconvert Strings greater than 500 characters to Text within @Embed collections." +
								"  Use Text for the field type instead." +
								"  You tried to save: " + value));
					
					return new Text(value);
				} else {
					return value;
				}
			}
		};
	}
}