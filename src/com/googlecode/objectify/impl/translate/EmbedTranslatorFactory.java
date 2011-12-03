package com.googlecode.objectify.impl.translate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.annotation.AlsoLoad;
import com.googlecode.objectify.annotation.Embed;
import com.googlecode.objectify.impl.Node;
import com.googlecode.objectify.impl.Path;
import com.googlecode.objectify.impl.Property;
import com.googlecode.objectify.impl.TypeUtils;
import com.googlecode.objectify.impl.translate.CollectionTranslatorFactory.CollectionListNodeTranslator;
import com.googlecode.objectify.impl.translate.MapTranslatorFactory.MapMapNodeTranslator;
import com.googlecode.objectify.repackaged.gentyref.GenericTypeReflector;


/**
 * <p>Translator which can map whole embedded classes.  Note that root classes are just a special case of an embedded
 * class, so there is a method here to create a root translator.</p>
 * 
 * @author Jeff Schnitzer <jeff@infohazard.org>
 */
public class EmbedTranslatorFactory<T> implements TranslatorFactory<T>
{
	/** Exists only so that we can get an array with the @Embed annotation */
	@Embed
	private static class HasEmbed {}
	
	/** Associates a Property with a Translator */
	private static class EachProperty {
		Property property;
		Translator<Object> translator;
		
		@SuppressWarnings("unchecked")
		public EachProperty(Property prop, Translator<?> trans) {
			this.property = prop;
			this.translator = (Translator<Object>)trans;
		}
		
		/** This is easier to debug if we have a string value */
		@Override
		public String toString() {
			return this.getClass().getSimpleName() + "(" + property.getName() + ")";
		}
		
		/** Executes loading this value from the node and setting it on the field */
		@SuppressWarnings({ "rawtypes", "unchecked" })
		public void executeLoad(Node node, Object onPojo, LoadContext ctx) {
			Node actual = getChild(node, property);
			
			// We only execute if there is a real node.  Note that even a null value in the data will have a real
			// EntityNode with a propertyValue of null, so this is a legitimate test for data in the source Entity
			if (actual != null) {
				try {
					// We have a couple special cases - for collection/map fields we would like to preserve the original
					// instance, if one exists.  It might have been initialized with custom comparators, etc.
					Object value;
					if (translator instanceof CollectionListNodeTranslator && actual.hasList()) {
						Collection coll = (Collection)this.property.get(onPojo);
						value = ((CollectionListNodeTranslator)translator).loadListIntoExistingCollection((Node)actual, ctx, coll);
					}
					else if (translator instanceof MapMapNodeTranslator && actual.hasMap()) {
						Map map = (Map)this.property.get(onPojo);
						value = ((MapMapNodeTranslator)translator).loadMapIntoExistingMap((Node)actual, ctx, map);
					}
					else {
						value = translator.load(actual, ctx);
					}
					
					property.set(onPojo, value);
				}
				catch (SkipException ex) {
					// No prob, skip this one
				}
			}
		}
		
		/** 
		 * Executes saving the field value from the pojo into the mapnode
		 * @param onPojo is the parent pojo which holds the property we represent
		 * @param node is the node that corresponds to the parent pojo; we create a new node and put it in here
		 * @param index is the default state of indexing up to this point 
		 */
		public void executeSave(Object onPojo, Node node, boolean index, SaveContext ctx) {
			if (property.isSaved(onPojo)) {
				// Look for an override on indexing
				Boolean propertyIndexInstruction = property.getIndexInstruction(onPojo);
				if (propertyIndexInstruction != null)
					index = propertyIndexInstruction;
				
				Object value = property.get(onPojo);
				try {
					Path path = node.getPath().extend(property.getName());
					Node child = translator.save(value, path, index, ctx);
					node.put(property.getName(), child);
				}
				catch (SkipException ex) {
					// No problem, do nothing
				}
			}
		}
		
		/**
		 * @param parent is the collection in which to look
		 * @param names is a list of names to look for in the parent 
		 * @return one child which has a name in the parent
		 * @throws IllegalStateException if there are multiple name matches
		 */
		private Node getChild(Node parent, Property prop) {
			Node child = null;
			
			for (String name: prop.getLoadNames()) {
				Node child2 = parent.get(name);
				
				if (child != null && child2 != null)
					throw new IllegalStateException("Collision trying to load field; multiple name matches for '"
							+ prop.getName() + "' at '" + child.getPath() + "' and '" + child2.getPath() + "'");
				
				if (child2 != null)
					child = child2;
			}
			
			return child;
		}
	}
	
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
			
			final List<EachProperty> props = new ArrayList<EachProperty>();
			
			for (Property prop: TypeUtils.getProperties(fact, clazz)) {
				Path propPath = path.extend(prop.getName());
				Translator<?> loader = fact.getTranslators().create(propPath, prop.getAnnotations(), prop.getType(), ctx);
				props.add(new EachProperty(prop, loader));
				
				// Sanity check here
				if (prop.hasIgnoreSaveConditions() && ctx.isInCollection() && ctx.isInEmbed())	// of course we're in embed
					propPath.throwIllegalState("You cannot use conditional @IgnoreSave within @Embed collections. @IgnoreSave is only allowed without conditions.");
			}
			
			return new MapNodeTranslator<T>() {
				@Override
				protected T loadMap(Node node, LoadContext ctx) {
					T pojo = fact.construct(clazz);
					
					for (EachProperty prop: props)
						prop.executeLoad(node, pojo, ctx);
					
					return pojo;
				}
	
				@Override
				protected Node saveMap(T pojo, Path path, boolean index, SaveContext ctx) {
					Node node = new Node(path);
					
					for (EachProperty prop: props)
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
