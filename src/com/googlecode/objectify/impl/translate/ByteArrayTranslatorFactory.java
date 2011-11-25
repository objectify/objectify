package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;


/**
 * Translates a byte[] to Blob.  Make sure this translator gets registered *before* the normal ArrayTranslator
 * otherwise it won't get used.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ByteArrayTranslatorFactory extends ValueTranslatorFactory<byte[], Blob>
{
	private static final Class<? extends byte[]> BYTE_ARRAY_TYPE = new byte[0].getClass();
	
	public ByteArrayTranslatorFactory() {
		super(BYTE_ARRAY_TYPE);
	}
	
	@Override
	public ValueTranslator<byte[], Blob> createSafe(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {

		return new ValueTranslator<byte[], Blob>(path, Blob.class) {
			@Override
			public byte[] loadValue(Blob value, LoadContext ctx) {
				return value.getBytes();
			}
			
			@Override
			protected Blob saveValue(byte[] value, SaveContext ctx) {
				return new Blob(value);
			}
		};
	}
}
