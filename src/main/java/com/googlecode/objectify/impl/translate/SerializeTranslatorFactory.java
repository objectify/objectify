package com.googlecode.objectify.impl.translate;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.impl.Path;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;


/**
 * <p>Loader which can load any serialized thing from a Blob.</p>
 *
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SerializeTranslatorFactory implements TranslatorFactory<Object, Object>
{
	private static final Logger log = Logger.getLogger(SerializeTranslatorFactory.class.getName());

	@Override
	public Translator<Object, Object> create(TypeKey<Object> tk, CreateContext ctx, Path path) {
		final Serialize serializeAnno = tk.getAnnotationAnywhere(Serialize.class);

		// We only work with @Serialize classes
		if (serializeAnno == null)
			return null;

		return new ValueTranslator<Object, Object>(Object.class) {
			@Override
			protected Object loadValue(Object value, LoadContext ctx, Path path) throws SkipException {
				final byte[] bytes = ByteArrayTranslatorFactory.getBytesFromBlob(value);

				// Need to be careful here because we don't really know if the data was serialized or not.  Start
				// with whatever the annotation says, and if that doesn't work, try the other option.
				try {
					ByteArrayInputStream bais = new ByteArrayInputStream(bytes);

					// Start with the annotation
					boolean unzip = serializeAnno.zip();
					try {
						return readObject(bais, unzip);
					} catch (IOException ex) {	// will be one of ZipException or StreamCorruptedException
						if (log.isLoggable(Level.INFO))
							log.log(Level.INFO, "Error trying to deserialize object using unzip=" + unzip + ", retrying with " + !unzip, ex);

						unzip = !unzip;
						return readObject(bais, unzip);	// this will pass the exception up
					}
				} catch (Exception ex) {
					path.throwIllegalState("Unable to deserialize " + value, ex);
					return null;	// never gets here
				}
			}

			@Override
			protected Object saveValue(Object value, boolean index, SaveContext ctx, Path path) throws SkipException {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					OutputStream out = baos;

					if (serializeAnno.zip()) {
						Deflater deflater = new Deflater(serializeAnno.compressionLevel());
						out = new DeflaterOutputStream(out, deflater);
					}

					ObjectOutputStream oos = new ObjectOutputStream(out);
					oos.writeObject(value);
					oos.close();

					return new Blob(baos.toByteArray());

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
