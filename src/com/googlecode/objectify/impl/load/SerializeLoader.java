package com.googlecode.objectify.impl.load;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which can load any serialized thing from a Blob.</p>
 */
public class SerializeLoader implements LoaderFactory<Object>
{
	@Override
	public Loader<Object> create(final ObjectifyFactory fact, Path path, Annotation[] fieldAnnotations, Type type) {

		final Class<?> clazz = (Class<?>)GenericTypeReflector.erase(type);
		
		// We only work with @Serialize classes
		if (TypeUtils.getAnnotation(Serialize.class, fieldAnnotations, clazz) == null)
			return null;
		
		// Sanity check so we don't have @Serialize and @Embed
		if (TypeUtils.getAnnotation(Embed.class, fieldAnnotations, clazz) == null)
			throw new IllegalStateException("You cannot both @Serialize and @Embed " + path);
		
		return new LoaderPropertyValue<Object, Blob>(path, Blob.class) {
			@Override
			public Object load(Blob value, LoadContext ctx) {
				try
				{
					ByteArrayInputStream bais = new ByteArrayInputStream(value.getBytes());
					ObjectInputStream ois = new ObjectInputStream(bais);
					
					return ois.readObject();
				}
				catch (Exception ex) { return path.throwIllegalState("Unable to deserialize " + value, ex); }
			}
		};
	}
}
