package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;

import java.net.MalformedURLException;
import java.net.URL;

/**
 * The datastore can't store URL, so translate it to a String and back.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org> 
 */
public class URLTranslatorFactory extends SimpleTranslatorFactory<URL, String>
{
	/** */
	public URLTranslatorFactory() {
		super(URL.class, ValueType.STRING);
	}

	@Override
	protected URL toPojo(final Value<String> value) {
		try {
			return new URL(value.get());
		} catch (MalformedURLException ex) {
			throw new RuntimeException(ex);
		}
	}

	@Override
	protected Value<String> toDatastore(final URL value) {
		return StringValue.of(value.toString());
	}
}