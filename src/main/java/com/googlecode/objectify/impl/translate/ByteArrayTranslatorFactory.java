package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.impl.Path;


/**
 * Translates a byte[] to Blob.  Make sure this translator gets registered *before* the normal ArrayTranslator
 * otherwise it won't get used.
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class ByteArrayTranslatorFactory extends ValueTranslatorFactory<byte[],Blob> {

	/** The pojo type this factory recognizes */
	private static final Class<? extends byte[]> BYTE_ARRAY_TYPE = byte[].class;

	/**
	 */
	protected ByteArrayTranslatorFactory() {
		super(BYTE_ARRAY_TYPE);
	}

	/* */
	@Override
	protected ValueTranslator<byte[], Blob> createValueTranslator(TypeKey<byte[]> tk, CreateContext ctx, Path path) {
		return new ValueTranslator<byte[], Blob>(Blob.class) {
			@Override
			public byte[] loadValue(Blob node, LoadContext ctx, Path path) throws SkipException {
				return node.getBytes();
			}

			@Override
			public Blob saveValue(byte[] pojo, boolean index, SaveContext ctx, Path path) throws SkipException {
				return new Blob(pojo);
			}
		};
	}
}
