package com.googlecode.objectify.impl.load;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.SaveContext;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load any serialized thing from a Blob.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class SerializeTranslatorFactory implements TranslatorFactory<Object>
{
	@Override
	public Translator<Object> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {

		final Class<?> clazz = (Class<?>)GenericTypeReflector.erase(type);
		
		// We only work with @Serialize classes
		if (TypeUtils.getAnnotation(Serialize.class, fieldAnnotations, clazz) == null)
			return null;
		
		// Sanity check so we don't have @Serialize and @Embed
		if (TypeUtils.getAnnotation(Embed.class, fieldAnnotations, clazz) == null)
			path.throwIllegalState("You cannot both @Serialize and @Embed; check the field and the target class for annotations");
		
		return new AbstractValueTranslator<Object, Blob>(path, Blob.class) {
			@Override
			public Object loadValue(Blob value, LoadContext ctx) {
				try {
					ByteArrayInputStream bais = new ByteArrayInputStream(value.getBytes());
					ObjectInputStream ois = new ObjectInputStream(bais);
					
					return ois.readObject();
					
				} catch (Exception ex) {
					path.throwIllegalState("Unable to deserialize " + value, ex);
					return null;	// never gets here
				}
			}

			@Override
			protected Blob saveValue(Object value, SaveContext ctx) {
				try {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					ObjectOutputStream oos = new ObjectOutputStream(baos);
					oos.writeObject(value);
					
					return new Blob(baos.toByteArray());
					
				} catch (IOException ex) {
					path.throwIllegalState("Unable to serialize " + value, ex);
					return null;	// never gets here
				}
			}
		};
	}
}
