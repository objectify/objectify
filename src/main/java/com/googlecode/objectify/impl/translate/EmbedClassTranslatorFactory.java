package com.googlecode.objectify.impl.translate;

import java.lang.reflect.Type;

import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which can map whole embedded classes.  This is appropriate to both class fields and the component
 * type of collection fields.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbedClassTranslatorFactory<T> implements TranslatorFactory<T>
{
	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.translate.TranslatorFactory#create(com.googlecode.objectify.impl.Path, java.lang.annotation.Annotation[], java.lang.reflect.Type, com.googlecode.objectify.impl.translate.CreateContext)
	 */
	@Override
	public Translator<T> create(final Path path, final Property property, final Type type, CreateContext ctx)
	{
		@SuppressWarnings("unchecked")
		final Class<T> clazz = (Class<T>)GenericTypeReflector.erase(type);
		
		// We only work with @Embed classes
		if (TypeUtils.getAnnotation(Embed.class, property, clazz) == null)
			return null;

		ctx.enterEmbed(path);
		try {
			// A little quirk is that we might have @AlsoLoad values on the embed, which means we might need to stuff some more
			// paths into the embedCollectionPoints.
			AlsoLoad alsoLoad = property.getAnnotation(AlsoLoad.class);
			if (alsoLoad != null)
				for (String name: alsoLoad.value())
					ctx.addAlternateEmbedPath(path.getPrevious().extend(name));
			
			return new ClassTranslator<T>(clazz, path, ctx);
		}
		finally {
			ctx.exitEmbed();
		}
	}
}
