package com.googlecode.objectify.impl.translate;

import com.google.cloud.datastore.Blob;
import com.google.cloud.datastore.BlobValue;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.ValueType;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.impl.Path;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


/**
 * <p>Loader which can load any serialized thing from a Blob.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
@Slf4j
public class SerializeTranslatorFactory implements TranslatorFactory<Object, Blob>
{
	@Override
	public Translator<Object, Blob> create(final TypeKey<Object> tk, final CreateContext ctx, final Path path) {
		final Serialize serializeAnno = tk.getAnnotationAnywhere(Serialize.class);

		// We only work with @Serialize classes
		if (serializeAnno == null)
			return null;

		return new ValueTranslator<Object, Blob>(ValueType.BLOB) {
			@Override

			protected Object loadValue(final Value<Blob> value, final LoadContext ctx, final Path path) throws SkipException {
				Exception initialException = null;
				// Need to be careful here because we don't really know if the data was serialized or not.  Start
				// with whatever the annotation says, and if that doesn't work, try the other option.
				try {
					final ByteArrayInputStream bais = new ByteArrayInputStream(value.get().toByteArray());

					// Start with the annotation's zip setting
					final boolean unzip = serializeAnno.zip();

					try {
						return readObject(bais, unzip);
					} catch (IOException ex) {	// will be one of ZipException or StreamCorruptedException

						initialException = ex;
						if (log.isInfoEnabled())
							log.info("Error trying to deserialize object using unzip=" + unzip + ", retrying with " + !unzip, ex);

						return readObject(bais, !unzip);	// this will pass the exception up
					}
				} catch (Exception ex) {
					if (initialException == null) {
						initialException = ex;
					}
					path.throwIllegalState("Unable to deserialize " + value, initialException);
					return null;	// never gets here
				}
			}

			@Override
			protected Value<Blob> saveValue(final Object value, final SaveContext ctx, final Path path) throws SkipException {
				try {
					final ByteArrayOutputStream baos = new ByteArrayOutputStream();
					OutputStream out = baos;

					if (serializeAnno.zip()) {
						final Deflater deflater = new Deflater(serializeAnno.compressionLevel());
						out = new DeflaterOutputStream(out, deflater);
					}

					final ObjectOutputStream oos = new ObjectOutputStream(out);
					oos.writeObject(value);
					oos.close();

					return BlobValue.of(Blob.copyFrom(baos.toByteArray()));

				} catch (IOException ex) {
					path.throwIllegalState("Unable to serialize " + value, ex);
					return null;	// never gets here
				}
			}

			/** Try reading an object from the stream */
			private Object readObject(ByteArrayInputStream bais, boolean unzip) throws IOException, ClassNotFoundException {
				bais.reset();
				InputStream in = bais;

				if (unzip)
					in = new InflaterInputStream(in);

				final ObjectInputStream ois = new ObjectInputStream(in);
				return ois.readObject();
			}
		};
	}
}
