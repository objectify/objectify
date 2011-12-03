package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TranslateProperty;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;
import com.googlecode.objectify.util.LogUtils;


/**
 * <p>Translator which can map whole embedded classes.  Note that root classes are just a special case of an embedded
 * class, so there is a method here to create a root translator.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbedTranslatorFactory<T> implements TranslatorFactory<T>
{
	/** */
	private static final Logger log = Logger.getLogger(EmbedTranslatorFactory.class.getName());
	
	/** Exists only so that we can get an array with the @Embed annotation */
	@Embed
	private static class HasEmbed {}
	
	/**
	 * Create a class loader for a root entity by making it work just like an embedded class.  Fakes
	 * the @Embed annotation as a fieldAnnotation. 
	 */
	public Translator<T> createRoot(Type type, CreateContext ctx) {
		return create(Path.root(), HasEmbed.class.getAnnotations(), type, ctx);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.LoaderFactory#create(com.googlecode.objectify.ObjectifyFactory, java.lang.reflect.Type, java.lang.annotation.Annotation[])
	 */
	@Override
	public Translator<T> create(final Path path, final Annotation[] fieldAnnotations, final Type type, CreateContext ctx)
	{
		@SuppressWarnings("unchecked")
		final Class<T> clazz = (Class<T>)GenericTypeReflector.erase(type);
		
		// We only work with @Embed classes
		if (TypeUtils.getAnnotation(Embed.class, fieldAnnotations) == null && clazz.getAnnotation(Embed.class) == null)
			return null;
		
		final ObjectifyFactory fact = ctx.getFactory();
		
		// Quick sanity check - can we construct one of these?  If not, blow up.
		try {
			fact.construct(clazz);
		} catch (Exception ex) {
			throw new IllegalStateException("Unable to construct an instance of " + clazz.getName() + "; perhaps it has no suitable constructor?", ex);
		}
		
		// Note that since we use this Translator to handle the root, the CreateContext is smart enough to ignore the
		// first call to enterEmbed().
		ctx.enterEmbed(path);
		try {
			// A little quirk is that we might have @AlsoLoad values on the embed, which means we might need to stuff some more
			// paths into the embedCollectionPoints.
			AlsoLoad alsoLoad = TypeUtils.getAnnotation(AlsoLoad.class, fieldAnnotations);
			if (alsoLoad != null)
				for (String name: alsoLoad.value())
					ctx.addAlternateEmbedPath(path.getPrevious().extend(name));
			
			final List<TranslateProperty<Object>> props = new ArrayList<TranslateProperty<Object>>();
			
			for (Property prop: TypeUtils.getProperties(fact, clazz)) {
				Path propPath = path.extend(prop.getName());
				Translator<Object> loader = fact.getTranslators().create(propPath, prop.getAnnotations(), prop.getType(), ctx);
				props.add(new TranslateProperty<Object>(prop, loader));
				
				// Sanity check here
				if (prop.hasIgnoreSaveConditions() && ctx.isInCollection() && ctx.isInEmbed())	// of course we're in embed
					propPath.throwIllegalState("You cannot use conditional @IgnoreSave within @Embed collections. @IgnoreSave is only allowed without conditions.");
			}
			
			return new MapNodeTranslator<T>() {
				@Override
				protected T loadMap(Node node, LoadContext ctx) {
					if (log.isLoggable(Level.FINEST))
						log.finest(LogUtils.msg(node.getPath(), "Instantiating a " + clazz.getName()));
						
					T pojo = fact.construct(clazz);
					
					for (TranslateProperty<Object> prop: props)
						prop.executeLoad(node, pojo, ctx);
					
					return pojo;
				}
	
				@Override
				protected Node saveMap(T pojo, Path path, boolean index, SaveContext ctx) {
					Node node = new Node(path);
					
					for (TranslateProperty<Object> prop: props)
						prop.executeSave(pojo, node, index, ctx);
					
					return node;
				}
			};
		}
		finally {
			ctx.exitEmbed();
		}
	}
}
