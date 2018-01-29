package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.ShortBlob;
import com.googlecode.objectify.impl.Path;


/**
 * <p>Translates a byte[] to Blob.  Make sure this translator gets registered *before* the normal ArrayTranslator
 * otherwise it won't get used.</p>
 *
 * <p>Also reads ShortBlob into the byte[]</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ByteArrayTranslatorFactory extends ValueTranslatorFactory<byte[], Object> {

	/** The pojo type this factory recognizes */
	private static final Class<? extends byte[]> BYTE_ARRAY_TYPE = byte[].class;

	/**
	 */
	protected ByteArrayTranslatorFactory() {
		super(BYTE_ARRAY_TYPE);
	}

	/* */
	@Override
	protected ValueTranslator<byte[], Object> createValueTranslator(TypeKey<byte[]> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<byte[], Object>(Object.class) {
			@Override
			public byte[] loadValue(Object node, LoadContext ctx, Path path) throws SkipException {
				return getBytesFromBlob(node);
			}

			@Override
			public Object saveValue(byte[] pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				return new Blob(pojo);
			}
		};
	}

	public static byte[] getBytesFromBlob(final Object node) {
		if (node instanceof Blob) {
			return ((Blob)node).getBytes();
		} else if (node instanceof ShortBlob) {
			return ((ShortBlob)node).getBytes();
		} else {
			throw new IllegalStateException("Can't convert " + node + " to a byte array");
		}
	}
}
