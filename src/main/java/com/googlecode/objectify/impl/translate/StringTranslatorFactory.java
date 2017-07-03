package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Text;
import com.googlecode.objectify.impl.Path;
import lombok.extern.java.Log;

import java.nio.charset.StandardCharsets;
import java.util.logging.Level;


/**
 * Knows how to convert Strings.  Datastore representation might be String or it might be Text.
 * Will work with anything that's in the datastore just by calling toString() on what we get back;
 * convenient for converting between say Number and the String representation, possibly dangerous
 * otherwise. 
 */
@Log
public class StringTranslatorFactory extends ValueTranslatorFactory<String, Object>
{
	/**
	 * Maximum number of BYTES we can store in a String before we have to convert to Text.
	 */
	public static final int MAX_STRING_BYTES = 1500;

	/**
	 * Google isn't explicit that UTF-8 encoding is used, but it's a safe assumption. Worst case is
	 * 4 bytes per character. So if we have more than that number of chars, we have to convert to
	 * UTF-8 to test the actual length.
	 */
	public static final int SAFE_STRING_CHARS = 1500 / 4;

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
				if (needsConversion(value)) {
					if (index)
						log.log(Level.WARNING, "Attempt to index a String which has been automatically converted to Text. The property is at " + path);

					return new Text(value);
				} else {
					return value;
				}
			}

			private boolean needsConversion(final String value) {
				if (value.length() < SAFE_STRING_CHARS)
					return false;

				final byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
				return bytes.length > MAX_STRING_BYTES;
			}
		};
	}
}