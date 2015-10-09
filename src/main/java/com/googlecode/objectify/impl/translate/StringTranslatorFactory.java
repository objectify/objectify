package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Knows how to convert Strings.  Datastore representation might be String or it might be Text.
 * Will work with anything that's in the datastore just by calling toString() on what we get back;
 * convenient for converting between say Number and the String representation, possibly dangerous
 * otherwise.
 */
public class StringTranslatorFactory extends ValueTranslatorFactory<String, Object>
{
	private static final Logger log = Logger.getLogger(StringTranslatorFactory.class.getName());

	/** */
	public StringTranslatorFactory() {
		super(String.class);
	}

	@Override
	protected ValueTranslator<String, Object> createValueTranslator(TypeKey<String> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<String, Object>(Object.class, String.class) {
			@Override
			protected String loadValue(Object value, LoadContext ctx, Path path) throws SkipException {
				if (value instanceof Text)
					return ((Text)value).getValue();
				else
					return value.toString();
			}

			@Override
			protected Object saveValue(String value, boolean index, SaveContext ctx, Path path) throws SkipException {
				// Check to see if it's too long and needs to be Text instead
				if (value.length() > 500) {
					if (index)
						log.log(Level.WARNING, "Attempt to index a String which has been automatically converted to Text. The property is at " + path);

					return new Text(value);
				} else {
					return value;
				}
			}
		};
	}
}
