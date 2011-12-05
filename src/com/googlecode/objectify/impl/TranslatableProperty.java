package com.googlecode.objectify.impl;

import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.googlecode.objectify.Result;
import com.googlecode.objectify.impl.translate.CollectionTranslatorFactory.CollectionListNodeTranslator;
import com.googlecode.objectify.impl.translate.LoadContext;
import com.googlecode.objectify.impl.translate.MapTranslatorFactory.MapMapNodeTranslator;
import com.googlecode.objectify.impl.translate.SaveContext;
import com.googlecode.objectify.impl.translate.SkipException;
import com.googlecode.objectify.impl.translate.Translator;
import com.googlecode.objectify.util.LogUtils;

/** 
 * Associates a Property with a Translator and provides a more convenient interface.
 */
public class TranslatableProperty<T> {
	/** */
	private static final Logger log = Logger.getLogger(TranslatableProperty.class.getName());
	
	/** */
	protected Property property;
	protected Translator<T> translator;
	
	/** */
	public TranslatableProperty(Property prop, Translator<T> trans) {
		this.property = prop;
		this.translator = trans;
	}
	
	/** */
	public Property getProperty() { return this.property; }
	
	/** This is easier to debug if we have a string value */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "(" + property.getName() + ")";
	}
	
	/** 
	 * Gets the appropriate value from the node and sets it on the appropriate field of the pojo.
	 * @param node is the container node for the property
	 * @param onPojo is the container object for the property
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void executeLoad(Node node, Object onPojo, LoadContext ctx) {
		Node actual = getChild(node, property);
		
		// We only execute if there is a real node.  Note that even a null value in the data will have a real
		// EntityNode with a propertyValue of null, so this is a legitimate test for data in the source Entity
		if (actual != null) {
			try {
				// We have a couple special cases - for collection/map fields we would like to preserve the original
				// instance, if one exists.  It might have been initialized with custom comparators, etc.
				T value;
				if (translator instanceof CollectionListNodeTranslator && actual.hasList()) {
					Collection<Object> coll = (Collection<Object>)this.property.get(onPojo);
					CollectionListNodeTranslator collTranslator = (CollectionListNodeTranslator)translator;
					value = (T)collTranslator.loadListIntoExistingCollection(actual, ctx, coll);
				}
				else if (translator instanceof MapMapNodeTranslator && actual.hasMap()) {
					Map map = (Map)this.property.get(onPojo);
					MapMapNodeTranslator mapTranslator = (MapMapNodeTranslator)translator;
					value = (T)mapTranslator.loadMapIntoExistingMap(actual, ctx, map);
				}
				else {
					value = translator.load(actual, ctx);
				}
				
				setOnPojo(onPojo, value, ctx, node.getPath());
			}
			catch (SkipException ex) {
				// No prob, skip this one
			}
		}
	}
	
	/**
	 * Sets the property on the pojo to the value.  However, sensitive to the value possibly being a Result<?>
	 * wrapper, in which case it enqueues the set operation until the loadcontext is done.
	 */
	private void setOnPojo(final Object pojo, final T value, final LoadContext ctx, final Path path) {
		if (value instanceof Result) {
			if (log.isLoggable(Level.FINEST))
				log.finest(LogUtils.msg(path, "Delaying set property " + property.getName()));
				
			ctx.delay(new Runnable() {
				@Override
				public void run() {
					@SuppressWarnings("unchecked")
					T actualValue = ((Result<T>)value).now();
					setOnPojo(pojo, actualValue, ctx, path);
				}
				
				@Override
				public String toString() {
					return "(delayed Runnable to set " + property.getName() + ")";
				}
			});
		} else if (value instanceof Partial) {
			// Register the partial as something that maybe gets filled in later
			@SuppressWarnings("unchecked")
			T partial = ((Partial<T>)value).get();
			ctx.registerPartial(pojo, property, partial);
			setOnPojo(pojo, partial, ctx, path);
		} else {
			if (log.isLoggable(Level.FINEST))
				log.finest(LogUtils.msg(path, "Setting property " + property.getName() + " to " + value));
			
			property.set(pojo, value);
		}
	}
	
	/** */
	public void setValue(Object pojo, Object value, LoadContext ctx) {
		T obj = translator.load(Node.of(value), ctx);
		setOnPojo(pojo, obj, ctx, Path.root());
	}
	
	/** Comes out in datastore format */
	public Object getValue(Object pojo) {
		@SuppressWarnings("unchecked")
		T value = (T)property.get(pojo);
		return translator.save(value, Path.root(), false, new SaveContext(null)).getPropertyValue();
	}
	
	/**
	 * Gets the appropriate field value from the pojo and puts it in the node with the appropriate key for this
	 * property. 
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
			
			@SuppressWarnings("unchecked")
			T value = (T)property.get(onPojo);
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
	 * Gets a child node from a parent, detecting collisions and throwing an exception if one is found.
	 * 
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