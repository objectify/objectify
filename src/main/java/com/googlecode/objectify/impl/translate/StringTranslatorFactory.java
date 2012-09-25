package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;


/**
 * Knows how to convert Strings.  Datastore representation might be String or it might be Text.
 * Will work with anything that's in the datastore just by calling toString() on what we get back;
 * convenient for converting between say Number and the String representation, possibly dangerous
 * otherwise. 
 */
public class StringTranslatorFactory extends ValueTranslatorFactory<String, Object>
{
	/** */
	public StringTranslatorFactory() {
		super(String.class);
	}
	
	@Override
	protected ValueTranslator<String, Object> createSafe(Path path, Property property, Type type, CreateContext ctx)
	{
		final boolean disallowConversion = ctx.isInCollection() && ctx.isInEmbed();
				
		return new ValueTranslator<String, Object>(path, Object.class) {
			@Override
			protected String loadValue(Object value, LoadContext ctx) {
				if (value instanceof Text)
					return ((Text)value).getValue();
				else
					return value.toString();
			}
			
			@Override
			protected Object saveValue(String value, SaveContext ctx) {
				// Check to see if it's too long and needs to be Text instead
				if (value.length() > 500) {
					if (disallowConversion)
						path.throwIllegalState("Objectify cannot autoconvert Strings greater than 500 characters to Text within @Embed collections." +
								"  Use Text for the field type instead." +
								"  You tried to save: " + value);
					
					return new Text(value);
				} else {
					return value;
				}
			}
		};
	}
}