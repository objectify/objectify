package com.googlecode.objectify.impl.conv;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;

/**
 * Converts a byte[] to Blob.  Make sure this converter gets registered in the standard converters
 * *before* the normal ArrayConverter otherwise it won't get used.
 */
public class ByteArrayConverter implements ConverterFactory<byte[], Blob>
{
	@Override
	public Converter<byte[], Blob> create(Type type, ConverterCreateContext ctx, StandardConversions conv) {
		
		if (GenericTypeReflector.getArrayComponentType(type) == Byte.TYPE)	{ // only the primitive, not the Byte
			return new Converter<byte[], Blob>() {
				/* */
				@Override
				public byte[] toPojo(Blob value, ConverterLoadContext ctx) {
					return value.getBytes();
				}
				
				/* */
				@Override
				public Blob toDatastore(byte[] value, ConverterSaveContext ctx) {
					return new Blob(value);
				}
			};
		} else {
			return null;
		}
	}
}