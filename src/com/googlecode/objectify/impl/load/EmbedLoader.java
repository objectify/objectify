package com.googlecode.objectify.impl.load;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.impl.LoadContext;
import com.googlecode.objectify.impl.Loadable;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.node.EntityNode;
import com.googlecode.objectify.impl.node.MapNode;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Loader which discovers how to load a class.  Can handle both the root class (because the key fields get stuffed
 * into the EntityNode) and simple (non-collection) embedded classes (which don't recognze @Id/@Parent as significant).</p>
 */
public class EmbedLoader<T> implements LoaderFactory<T>
{
	/** Exists only so that we can get an array with the @Embed annotation */
	@Embed
	private static class HasEmbed {}
	
	/** Associates a Loader with a Loadable*/
	private static class FieldLoader {
		Loadable loadable;
		Loader<?> loader;
		
		public FieldLoader(Loadable loadable, Loader<?> loader) {
			this.loadable = loadable;
			this.loader = loader;
		}
		
		/** Executes loading this value from the node and setting it on the field */
		public void execute(Object pojo, MapNode node, LoadContext ctx) {
			EntityNode actual = getChild(node, loadable.getNames());
			
			// We only execute if there is a real node.  Note that even a null value in the data will have a real
			// MapNode with a propertyValue of null.
			
			if (actual != null) {
				Object value = loader.load(actual, ctx);
				loadable.set(pojo, value);
			}
		}
		
		/**
		 * @param parent is the collection in which to look
		 * @param names is a list of names to look for in the parent 
		 * @return one child which has a name in the parent
		 * @throws IllegalStateException if there are multiple name matches
		 */
		private EntityNode getChild(MapNode parent, String[] names) {
			EntityNode child = null;
			
			for (String name: names) {
				EntityNode child2 = parent.get(name);
				
				if (child != null && child2 != null)
					throw new IllegalStateException("Collision trying to load field; multiple name matches at " + parent.getPath());
				
				child = child2;
			}
			
			return child;
		}
	}
	
	/**
	 * Create a class loader for a root entity by making it work just like an embedded class.  Fakes
	 * the @Embed annotation as a fieldAnnotation. 
	 */
	public Loader<T> createRoot(ObjectifyFactory fact, Type type) {
		return create(fact, Path.root(), HasEmbed.class.getAnnotations(), type);
	}

	/* (non-Javadoc)
	 * @see com.googlecode.objectify.impl.load.LoaderFactory#create(com.googlecode.objectify.ObjectifyFactory, java.lang.reflect.Type, java.lang.annotation.Annotation[])
	 */
	@Override
	public Loader<T> create(final ObjectifyFactory fact, final Path path, final Annotation[] fieldAnnotations, final Type type)
	{
		@SuppressWarnings("unchecked")
		final Class<T> clazz = (Class<T>)GenericTypeReflector.erase(type);
		
		// We only work with @Embed classes
		if (TypeUtils.getAnnotation(Embed.class, fieldAnnotations) == null && clazz.getAnnotation(Embed.class) == null)
			return null;
		
		final List<FieldLoader> fieldLoaders = new ArrayList<FieldLoader>();
		
		for (Loadable loadable: TypeUtils.getLoadables(clazz)) {
			Path loaderPath = path.extend(loadable.getPathName());
			Loader<?> loader = fact.getLoaders().create(loaderPath, loadable.getAnnotations(), loadable.getType());
			fieldLoaders.add(new FieldLoader(loadable, loader));
		}
		
		return new LoaderMapNode<T>(path) {
			@Override
			public T load(MapNode node, LoadContext ctx)
			{
				T pojo = fact.construct(clazz);
				
				for (FieldLoader fieldLoader: fieldLoaders)
					fieldLoader.execute(pojo, node, ctx);
				
				return pojo;
			}
			
		};
	}
}
